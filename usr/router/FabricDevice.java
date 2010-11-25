package usr.router;

import usr.net.*;
import usr.logging.*;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/** A fabric device is a device with an Inbound and an outbound queue.
Inboud is "towards fabric" and Outbound is "from fabric" -- see diagram
Different queueing disciplines can be chosen for incoming and outgoing 
queues
 */
public class FabricDevice implements FabricDeviceInterface {
    
    String name_= "Unnamed Fabric Device";  // Device name
    NetIFListener listener_;  //  NetIF listener for this device
    Thread inThread_= null;  // Thread for incoming queue
    Thread outThread_= null;  // Thread for outgoing queue
    boolean stopped_= true;

    static final int QUEUE_BLOCKING=1;  // If queue is over size then queue wait objects and block
    static final int QUEUE_DROPPING=2;  // If queue is over size then drop packet
    static final int QUEUE_NOQUEUE=3;
    int inQueueLen_= 0;  // Zero will be interpreted as an no limit queue
    int outQueueLen_= 0;
    int inQueueDiscipline_= QUEUE_NOQUEUE;
    int outQueueDiscipline_= QUEUE_NOQUEUE;

    LinkedBlockingQueue <DatagramHandle> inQueue_= null;  // queue for inbound -- towards fabric
    LinkedBlockingQueue <DatagramHandle> outQueue_= null;  // queue for outgoing -- away from fabric
    DatagramDevice device_;
    InQueueHandler inQueueHandler_= null;
    OutQueueHandler outQueueHandler_= null;
        // counts
    NetStats netStats_= null;
    
    LinkedList<Object> inWaitQueue_= null;  // Queue stores objects waiting for 
    LinkedList<Object> outWaitQueue_= null;  // notification from blocking queue
   
    int inq=0;
    boolean started_= false;
    
    /** Default fabric device has no queue */
    public FabricDevice (DatagramDevice ep, NetIFListener l)
    {
        device_= ep;
        listener_= l;
        netStats_ = new NetStats();
    }
    
    /** Set the queue type for the inbound queue */
    public void setInQueueDiscipline(int discipline) 
    {
        if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " cannot change queue after fabric start");
            return;
        }
        if (discipline == QUEUE_BLOCKING || discipline == QUEUE_DROPPING ||
            discipline == QUEUE_NOQUEUE) {
            inQueueDiscipline_= discipline;    
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " illegal queue discipline for in queue");
        }
    }
    
    /** Set the queue type for the outbound queue */
    public void setOutQueueDiscipline(int discipline) 
    {
        if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " cannot change queue after fabric start");
            return;
        }
        if (discipline == QUEUE_BLOCKING || discipline == QUEUE_DROPPING ||
            discipline == QUEUE_NOQUEUE) {
            outQueueDiscipline_= discipline;    
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " illegal queue discipline for out queue");
        }
    
    }
    
    /** Set the queue length for the inbound queue */
    public void setInQueueLength(int len) 
    {
         if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " cannot change queue after fabric start");
            return;
        }
        if (len >= 0) {
            inQueueLen_= len;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " illegal queue len");
            return;    
        } 
    }
    
    /** Set the queue length for the outbound queue */
    public void setOutQueueLength(int len) 
    {
        if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " cannot change queue after fabric start");
            return;
        }
        if (len >= 0) {
            outQueueLen_= len;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                " illegal queue len");
            return;    
        } 
   
    }


    /*
    
    /** Start inqueue and out queue threads if necessary */
    public synchronized void start() {
        stopped_= false;

        if (inQueueDiscipline_ != QUEUE_NOQUEUE) {
            
            if (inQueueLen_ == 0) {
                inQueue_= new LinkedBlockingQueue<DatagramHandle>();
            } else {
                inQueue_= new LinkedBlockingQueue<DatagramHandle>(inQueueLen_);
            }
            inQueueHandler_ = new InQueueHandler(inQueueDiscipline_, inQueue_,this);
            inWaitQueue_= new LinkedList<Object>();
            inThread_= new Thread(inQueueHandler_,name_+"-inQueue");
            inThread_.start();
        }
        if (outQueueDiscipline_ != QUEUE_NOQUEUE) {
            if (outQueueLen_ == 0) {
                outQueue_= new LinkedBlockingQueue<DatagramHandle>();
            } else {
                outQueue_= new LinkedBlockingQueue<DatagramHandle>(outQueueLen_);                
            }
            outWaitQueue_= new LinkedList<Object>();
            outQueueHandler_ = new OutQueueHandler(outQueueDiscipline_, outQueue_, this);
            outThread_= new Thread(outQueueHandler_,name_+"-outQueue");
            outThread_.start();
        }
    }
    
    /** Is the output queue blockinig */
    boolean outIsBlocking() {
        return (outQueueDiscipline_ == QUEUE_BLOCKING);
    }
    
    /** Is the input queue blockinig */
    boolean inIsBlocking() {
        return (inQueueDiscipline_ == QUEUE_BLOCKING);
    }
    
    /** Register stats using these functions */
    void inDroppedPacket(Datagram dg) 
    {
        
        netStats_.increment(NetStats.Stat.InDropped);
        if (inIsBlocking()) {
           Logger.getLogger("log").logln(USR.ERROR, leadin()+" in dropped packet");
        }
    }

    /** Register stats using these functions  --- packet dropped due to no route*/
    void inDroppedPacketNR(Datagram dg) 
    {
         netStats_.increment(NetStats.Stat.InDropped);
       
    }
    
    void outDroppedPacket(Datagram dg) 
    
    {
        netStats_.increment(NetStats.Stat.OutDropped);
        if (outIsBlocking()) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" out dropped packet");
        }
    }
    
    void inSentPacket(Datagram dg) 
    {
                // stats
        netStats_.increment(NetStats.Stat.InPackets);
        netStats_.add(NetStats.Stat.InBytes, dg.getTotalLength());
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" in sent "+inSent_);
    }
    
    void outSentPacket(Datagram dg) 
    {
        netStats_.increment(NetStats.Stat.OutPackets);
        netStats_.add(NetStats.Stat.OutBytes, dg.getTotalLength());
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" out sent "+outSent_);
    }
    
    
    /** Strip first object from queue for waiting notifiers */
    Object getInWaitQueue() {
        if (inWaitQueue_ == null) 
            return null;
        try {
            Object o= inWaitQueue_.removeFirst();
            return o;
        } catch (NoSuchElementException e) {
        }
        return null;
    }
    
    /** Strip first object from queue for waiting notifiers */
    Object getOutWaitQueue() {
        try {
            Object o= outWaitQueue_.removeFirst();
            return o;
        } catch (NoSuchElementException e) {
        }
        return null;
    }
    
    /** Return the fabric device that this datagram may route to or no 
    such fabric */
    FabricDevice getRouteFabric(Datagram dg) {
        if (listener_ == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" no listener");
            return null;
        }
        return listener_.getRouteFabric(dg);
    }
    
    /** Add a datagram to the in queue -- blocks if it is a blocking queue*/
    public boolean blockingAddToInQueue(Datagram dg, DatagramDevice dd) 
      throws NoRouteToHostException{
        Object waitHere= new Object();
        while(true) {
            
            boolean processed= addToInQueue(dg, dd ,waitHere);
            if (processed)
                break;
            synchronized(waitHere) {
                try {
                    waitHere.wait(100);
                } catch (InterruptedException e) {
                }
            }
        }
        return true;
            
    }
    
   public synchronized boolean addToInQueue(Datagram dg, DatagramDevice dd) 
      throws NoRouteToHostException{
        return addToInQueue(dg,dd,null);
    }
    
    /** Add a datagram to the in queue -- true means packet has been "dealt with" -- dropped or sent
    false means "blocked" */
    public synchronized boolean addToInQueue(Datagram dg, DatagramDevice dd, Object waitObj) 
      throws NoRouteToHostException{
        
        DatagramHandle dh= new DatagramHandle(dg,dd);
        String name= "null";
        if (dd != null) {
            name= dd.getName();
        }
        if (inQueueDiscipline_ == QUEUE_NOQUEUE) {
            boolean processed= transferDatagram(dh);
            if (processed) {
                return true;
            }
            if (waitObj == null || outIsBlocking() == false) {
                inDroppedPacket(dg);
                return true;
            }
            // Here we have not processed the datagram -- the user may resend later
            return false;
        }
        // Queue the packet if possible
        if (inQueueLen_ == 0 || inQueue_.size() < inQueueLen_) {
            inQueue_.offer(dh);
            return true;
        }
        
        
        if (waitObj != null) {
            inWaitQueue_.addLast(waitObj);
            return false;
        }
        inDroppedPacket(dg);
        return true;
    }
    
    public NetStats getNetStats() {
        return netStats_;
    }
    
    
    /** Set the listener device for this fabric device */
    public void setListener(NetIFListener l) {
        listener_= l;
    }
    
    /** Get the listener device */
    public NetIFListener getListener() {
        return listener_;
    } 
    
      /** Add a datagram to the out queue -- return true if datagram processed (dropped or sent)*/
    public synchronized boolean addToOutQueue(DatagramHandle dh) throws NoRouteToHostException{
        return addToOutQueue(dh,null);
    }
    
    
        /** Add a datagram to the out queue --
        true means dealt with and accounted in stats
        false means not dealt with*/
    public synchronized boolean addToOutQueue(DatagramHandle dh, Object waitObj) throws NoRouteToHostException{
        if (dh.datagram.TTLReduce() == false) {
            listener_.TTLDrop(dh.datagram);
            inDroppedPacket(dh.datagram);
            return true;
        }
        if (outQueueDiscipline_ == QUEUE_NOQUEUE) {
             boolean processed= sendOutDatagram(dh);
             //sent
             if (processed) {
                  inSentPacket(dh.datagram);
                  outSentPacket(dh.datagram);
                  return true;
             }
             //blocked
             if (waitObj != null && outIsBlocking()) {
                  return false;
             }
             //dropped
             inSentPacket(dh.datagram);
             outDroppedPacket(dh.datagram);
             return true;
        }
        // Queue the packet if possible
        if (outQueueLen_ == 0 || outQueue_.size() < outQueueLen_) {
            outQueue_.offer(dh);
            inSentPacket(dh.datagram);
            return true;
        }
        // Packet is blocked -- do not know yet if it is sent or not
        if (waitObj != null && outIsBlocking()) {
            outWaitQueue_.addLast(waitObj);
            return false;
        } 
        inSentPacket(dh.datagram);
        outDroppedPacket(dh.datagram);
        return true;
    }
    
    /** transfer datagram from in queue to out queue using no queue discipline -- true
     means dealt with and accounted for in stats -- false means could not send*/
    public boolean transferDatagram(DatagramHandle dh) throws NoRouteToHostException{
        if (dh.datagram.TTLReduce() == false) {
            listener_.TTLDrop(dh.datagram);
            inDroppedPacket(dh.datagram);
            return true;
        }
        FabricDevice f= getRouteFabric(dh.datagram);
        if (f == null) {
            inDroppedPacketNR(dh.datagram); 
            throw (new NoRouteToHostException());
        }
        boolean sent= f.addToOutQueue(dh, null);
        if (sent) {
            inSentPacket(dh.datagram);
        }
        return sent;
    }
    
    /** Send the outbound Datagram onwards */
    public synchronized boolean sendOutDatagram(DatagramHandle dh) {
        Datagram dg= dh.datagram;
        DatagramDevice dd= dh.datagramDevice;
        String name= "null";
        if (dd != null) {
            name= dd.getName();
        }
        boolean sent= device_.outQueueHandler(dh.datagram, dh.datagramDevice);
        if (sent) {
            outSentPacket(dh.datagram);
        } else {
            outDroppedPacket(dh.datagram);
        }
        return sent;
    }
    
    /** Is the inbound queue currently full */
    public synchronized boolean inQueueFull() {
        if (inQueueDiscipline_ == QUEUE_NOQUEUE) 
            return false;
        if (inQueueLen_ == 0)
            return false;
        if (inQueue_.size() < inQueueLen_) 
            return false;
        return true;
    }
    
    /** Is the outbbound queue currently full */
    public boolean outQueueFull() {
        if (outQueueDiscipline_ == QUEUE_NOQUEUE) 
            return false;
        if (outQueueLen_ == 0)
            return false;
        if (outQueue_.size() < inQueueLen_) 
            return false;
        return true;
    }
    
    /** Stop any running threads */
    public void stop() {
        
        stopped_= true;
        if (inQueueHandler_ != null) {
            inQueueHandler_.stopThread();
        }
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" in queue stop");
        if (outQueueHandler_ != null) {
            outQueueHandler_.stopThread();
        }
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" out queue stop");
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+" fabric device stop");
        
    }
    
    void setName(String name) 
    {
        name_= name;
        if (inThread_ != null) {
            inThread_.setName(name+"-InQueue");
        }
        if (outThread_ != null) {
            outThread_.setName(name+"-OutQueue");
        }
       
    }
     
    String getName() {
        return name_;
    }
    
    String leadin() {
        return "FD: "+name_+": ";
    }
}

class InQueueHandler implements Runnable {
   
    int queueDiscipline_= 0;
    LinkedBlockingQueue <DatagramHandle> queue_= null;
    FabricDevice fabricDevice_= null;
    boolean running_ = false;
    Thread runThread_= null;
    Object blockWaitObj_;  // This object is used to wait when a blocking queue is sent to
    String name_;   
    
    /** Constructor sets up */
    InQueueHandler(int discipline, LinkedBlockingQueue<DatagramHandle> q, 
      FabricDevice f) {
        queueDiscipline_= discipline;
        queue_= q;
        fabricDevice_= f;
        name_= f.getName();
        blockWaitObj_= new Object();
    }
    
    public void run() {
        running_= true;
        runThread_= Thread.currentThread();
        int ct= 0;
        while (running_ || queue_.size() > 0) {
            // Consider waking next in line if we're a blocking queue
            if (running_ &&  fabricDevice_.inQueueFull() == false) {
               Object wake= fabricDevice_.getInWaitQueue();
               if (wake != null) {
                  synchronized(wake) {
                      wake.notify();
                  }
               }
            }
            // Get a datagram from the queue
            DatagramHandle dh= null;
            
            try {
                dh = queue_.take();
            } catch (InterruptedException e) {
                break;  // Interrupt should only occur when queue is zero
            }
            FabricDevice f= fabricDevice_.getRouteFabric(dh.datagram);
            if (f == null) { //  Cannot route
                fabricDevice_.inDroppedPacketNR(dh.datagram);
                continue;
            }
            while (true) {  // Attempt to send the datagram to the correct queue
                boolean sent;
                try {
                    sent= f.addToOutQueue(dh,blockWaitObj_);
                } catch (NoRouteToHostException e) {
                    fabricDevice_.inDroppedPacketNR(dh.datagram);
                    break;
                }
                // If the packet was sent, the output is not blocking or
                // we are in shut down then drop the packet
                if (sent) {
                    fabricDevice_.inSentPacket(dh.datagram);
                    break;
                }
                if (f.outIsBlocking() == false) {
                    fabricDevice_.inDroppedPacket(dh.datagram);
                    break;
                }
                synchronized(blockWaitObj_) { // Blocking out queue -- wait
                   
                    try {
                        blockWaitObj_.wait(1000);
                    } catch (InterruptedException e) {
                        
                    }
                }
            }
            
            
        }
    }
    
    public synchronized void stopThread() {
    
        running_= false;
        if (queue_.size() == 0) {
            runThread_.interrupt();
        }
        try {
            runThread_.join();
        } catch (InterruptedException e) {
        }
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"thread stopped");
    }
    
    String leadin() {
        return "FDInQ "+name_+": ";
    }
}

class OutQueueHandler implements Runnable {
    int queueDiscipline_= 0;
    LinkedBlockingQueue <DatagramHandle> queue_;
    FabricDevice fabricDevice_= null;
    volatile boolean outRunning_= false;
    Thread outThread_= null;
    String name_= null;
    
    OutQueueHandler(int discipline, LinkedBlockingQueue<DatagramHandle> q,
      FabricDevice f) {
        queueDiscipline_= discipline;
        queue_= q;
        fabricDevice_= f;
        name_= f.getName();
    }
    
    
    public void run() {
        outRunning_= true;
        outThread_= Thread.currentThread();
        while (outRunning_ || queue_.size() > 0) {
            if (fabricDevice_.outQueueFull() == false) {
                Object wake= fabricDevice_.getOutWaitQueue();
                if (wake != null) {
                    synchronized(wake) { 
                       wake.notify();
                   }
               }
            }
            DatagramHandle dh= null;
            try {
                dh= queue_.take();
            } catch (InterruptedException e) {
                continue;
            }
            if (dh != null) {
                fabricDevice_.outSentPacket(dh.datagram);
                fabricDevice_.sendOutDatagram(dh);
            }
        }
    }
    
    public synchronized void stopThread() {
        if (outRunning_ == false)
            return;
        
        outRunning_= false;
        if (queue_.size() == 0) {
            outThread_.interrupt();
        }
        try {
            outThread_.join();
        } catch (InterruptedException e) {
            
        }
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"thread stopped");
    }
    
    String leadin() {
        return "FDOutQ "+name_+": ";
    }
}

class DatagramHandle {
    public Datagram datagram;
    public DatagramDevice datagramDevice;
    
    DatagramHandle(Datagram dg, DatagramDevice d) {
        datagram= dg;
        datagramDevice= d;
    }
}
