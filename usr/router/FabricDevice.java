package usr.router;

import usr.net.*;
import usr.logging.*;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

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

    int maxInQueue_ = 0;
    int maxOutQueue_= 0;

    LinkedBlockingDeque <DatagramHandle> inQueue_= null;  // queue for inbound -- towards fabric
    LinkedBlockingDeque <DatagramHandle> outQueue_= null;  // queue for outgoing -- away from fabric
    DatagramDevice device_;
    InQueueHandler inQueueHandler_= null;
    OutQueueHandler outQueueHandler_= null;
    // counts
    NetStats netStats_= null;

    LinkedBlockingDeque<Object> inWaitQueue_= null;  // Queue stores objects waiting for
    LinkedBlockingDeque<Object> outWaitQueue_= null;  // notification from blocking queue

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
        if (discipline == QUEUE_BLOCKING || 
            discipline == QUEUE_DROPPING ||
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
        if (discipline == QUEUE_BLOCKING || 
            discipline == QUEUE_DROPPING ||
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
                inQueue_= new LinkedBlockingDeque<DatagramHandle>();
            } else {
                inQueue_= new LinkedBlockingDeque<DatagramHandle>(inQueueLen_);
            }
            inWaitQueue_= new LinkedBlockingDeque<Object>();
            inQueueHandler_ = new InQueueHandler(inQueueDiscipline_, inQueue_,
                                                 inWaitQueue_,this);

            inThread_= new Thread(inQueueHandler_,name_+"-inQueue");
            inThread_.start();
        }
        if (outQueueDiscipline_ != QUEUE_NOQUEUE) {
            if (outQueueLen_ == 0) {
                outQueue_= new LinkedBlockingDeque<DatagramHandle>();
            } else {
                outQueue_= new LinkedBlockingDeque<DatagramHandle>(outQueueLen_);
            }
            outWaitQueue_= new LinkedBlockingDeque<Object>();
            outQueueHandler_ = new OutQueueHandler(outQueueDiscipline_, outQueue_,
                                                   outWaitQueue_, this);
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

    /** Register stats using these functions  --- packet dropped but in expected manner*/
    void inDroppedPacketNR(Datagram dg)
    {
        netStats_.increment(NetStats.Stat.InDropped);

    }

    void outDroppedPacket(Datagram dg)

    {
        netStats_.increment(NetStats.Stat.OutDropped);
        //  This can happen on blocking out interface if interface is shut beforehand

    }

    void inSentPacket(Datagram dg)
    {
        // stats
        netStats_.increment(NetStats.Stat.InPackets);
        netStats_.add(NetStats.Stat.InBytes, dg.getTotalLength());
        if (dg.getProtocol() == Protocol.DATA) {
            netStats_.increment(NetStats.Stat.InDataPackets);
            netStats_.add(NetStats.Stat.InDataBytes, dg.getTotalLength());
        }
    }

    void outSentPacket(Datagram dg)
    {
        netStats_.increment(NetStats.Stat.OutPackets);
        netStats_.add(NetStats.Stat.OutBytes, dg.getTotalLength());
        if (dg.getProtocol() == Protocol.DATA) {
            netStats_.increment(NetStats.Stat.OutDataPackets);
            netStats_.add(NetStats.Stat.OutDataBytes, dg.getTotalLength());
        }

    }



    /** Return the fabric device that this datagram may route to or no
       such fabric */
    FabricDevice getRouteFabric(Datagram dg) throws NoRouteToHostException {
        if (listener_ == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" no listener");
            return null;
        }
        return listener_.getRouteFabric(dg);
    }

    /** Add a datagram to the in queue -- blocking call, will continue to wait until
        datagram is added */
    public boolean blockingAddToInQueue(Datagram dg, DatagramDevice dd)
    throws NoRouteToHostException {

        Object waitHere= new Object();
        synchronized (waitHere) {
            while(true) {

                try {
                    boolean processed= addToInQueue(dg, dd,waitHere);
                    return processed;
                }
                catch (usr.net.InterfaceBlockedException e) {
                    try {
                        waitHere.wait(500);
                    } catch (InterruptedException ie) {
                    }

                }
            }
        }
    }

    /** Returns true if datagram is sent or false if dropped */
    public boolean addToInQueue(Datagram dg, DatagramDevice dd)
    throws NoRouteToHostException {
        try {
            return addToInQueue(dg,dd,null);
        } catch (usr.net.InterfaceBlockedException e) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+

                                          " Interface Blocked Exception should not be thrown in addToInQueue");
        }
        return false; // This line should never be reached.
    }

    /** Add a datagram to the in queue -- true means sent to out queue, false means blocked */
    public boolean addToInQueue(Datagram dg, DatagramDevice dd, Object waitObj)
    throws NoRouteToHostException,  usr.net.InterfaceBlockedException {

        DatagramHandle dh= new DatagramHandle(dg,dd);

        if (inQueueDiscipline_ == QUEUE_NOQUEUE) {
            boolean processed= transferDatagram(dh);
            if (processed) {
                inSentPacket(dg);
                return true;
            }
            if (inIsBlocking() && waitObj != null) {
                throw new usr.net.InterfaceBlockedException();
            }
            inDroppedPacket(dg);
            return false;
        } else {
            // QUEUE_BLOCKING || QUEUE_DROPPING

            // Queue the packet if possible
            synchronized (inQueue_) {
                if (inQueueLen_ == 0 || inQueue_.size() < inQueueLen_) {
                    inQueue_.offerLast(dh);
                    if (inQueue_.size() > maxInQueue_) {
                        maxInQueue_= inQueue_.size();
                        netStats_.setValue(NetStats.Stat.BiggestInQueue,maxInQueue_);
                    }
                    return true;
                }
            }

            // Datagram blocked
            // So add it's waitObj to a queue of waiting senders
            // It will be notified when the queue is ready
            if (waitObj != null) {
                inWaitQueue_.addLast(waitObj);
                // Here we have not processed the datagram -- the user may resend later
                throw new usr.net.InterfaceBlockedException();
            } else {
                inDroppedPacket(dg);
                return true;
            }
        }
    }

    public NetStats getNetStats() {
        return (NetStats)netStats_.clone();
    }


    /** Set the listener device for this fabric device */
    public void setListener(NetIFListener l) {
        listener_= l;
    }

    /** Get the listener device */
    public NetIFListener getListener() {
        return listener_;
    }



    /** Add a datagram to the out queue -- return true if datagram
       added to out queue, false means rejected*/
    public boolean addToOutQueue(DatagramHandle dh) throws
    NoRouteToHostException, InterfaceBlockedException {
        return addToOutQueue(dh,null);
    }


    /** Add a datagram to the out queue --
       true means added to out queue, false means rejected*/
    public boolean addToOutQueue(DatagramHandle dh, Object waitObj)
    throws NoRouteToHostException, InterfaceBlockedException {

        if (outQueueDiscipline_ == QUEUE_NOQUEUE) {
            if (dh.datagram.TTLReduce() == false) {
                listener_.TTLDrop(dh.datagram);
                return false;
            }
            boolean processed= sendOutDatagram(dh);
            return true;

        } else {
            // QUEUE_BLOCKING || QUEUE_DROPPING

            // Queue the packet if possible
            synchronized (outQueue_) {
                if (outQueueLen_ == 0 || outQueue_.size() < outQueueLen_) {
                    outQueue_.offerLast(dh);
                    if (outQueue_.size() > maxOutQueue_) {
                        maxOutQueue_= outQueue_.size();
                        netStats_.setValue(NetStats.Stat.BiggestOutQueue,maxOutQueue_);
                    }
                    return true;
                }
            }

            // Packet is blocked
            // So add it's waitObj to a queue of waiting senders
            // It will be notified when the queue is ready
            if (waitObj != null && outIsBlocking()) {
                outWaitQueue_.addLast(waitObj);
                throw new InterfaceBlockedException();
            } else {
                return false;
            }
        }
    }

    /** transfer datagram from in queue to out queue using no queue discipline
        add right now or drop
     */
    public boolean transferDatagram(DatagramHandle dh) throws NoRouteToHostException {

        if (dh.datagram.TTLReduce() == false) {
            listener_.TTLDrop(dh.datagram);
            return false;
        }
        FabricDevice f= null;
        try {
            f= getRouteFabric(dh.datagram);
        } catch (NoRouteToHostException e) {
            throw (e);
        }
        Object waitObj= new Object();
        while (true) {
            try {
                return f.addToOutQueue(dh, waitObj);
            } catch (java.net.NoRouteToHostException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "NoRouteToHostException for transferDatagram " + dh.datagram);
                return false;
            } catch (usr.net.InterfaceBlockedException ex) {
                synchronized (waitObj) {
                    try {
                        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "transferDatagram WAIT 500");
                        waitObj.wait(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    /** Send the outbound Datagram onwards */
    public boolean sendOutDatagram(DatagramHandle dh) throws InterfaceBlockedException {
        Datagram dg= dh.datagram;
        DatagramDevice dd= dh.datagramDevice;

        boolean sent= device_.outQueueHandler(dh.datagram, dh.datagramDevice);
        if (sent) {
            outSentPacket(dh.datagram);
        } else {
            outDroppedPacket(dh.datagram);
        }
        return sent;
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
    LinkedBlockingDeque <DatagramHandle> queue_= null;
    LinkedBlockingDeque<Object> inWaitQueue_= null;
    FabricDevice fabricDevice_= null;
    Boolean running_ = false;
    Thread runThread_= null;
    Object blockWaitObj_;  // This object is used to wait when a blocking queue is sent to
    String name_;

    /** Constructor sets up */
    InQueueHandler(int discipline, LinkedBlockingDeque<DatagramHandle> q,
                   LinkedBlockingDeque<Object> wait, FabricDevice f) {
        queueDiscipline_= discipline;
        queue_= q;
        inWaitQueue_= wait;
        fabricDevice_= f;
        name_= f.getName();

    }

    public void run() {
        running_= true;
        runThread_= Thread.currentThread();
        int ct= 0;
        Object wake= null;

        while (running_ || queue_.size() > 0) {
            // Consider waking next in line if we're a blocking queue
            // WAS if (queue_.remainingCapacity() > 0 && inWaitQueue_ != null) {
            if (queue_.remainingCapacity() > 0 && inWaitQueue_ != null && inWaitQueue_.size() > 0) {
                try {
                    wake= inWaitQueue_.removeFirst();
                    synchronized (wake) {
                        wake.notify();
                    }
                }
                catch (NoSuchElementException e) {
                }

            }

            // Get a datagram from the queue
            DatagramHandle dh= null;

            try {
                dh = queue_.take();
            } catch (InterruptedException e) {
                break;  // Interrupt should only occur when queue is zero
            }
            FabricDevice f= null;
            try {
                f= fabricDevice_.getRouteFabric(dh.datagram);
            } catch (NoRouteToHostException e) { //  Cannot route
                fabricDevice_.inDroppedPacketNR(dh.datagram);
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"Cannot route " + dh.datagram);
                continue;
            }

            // get object to block on
            if (f.outIsBlocking()) {
                blockWaitObj_= new Object();
            } else {
                blockWaitObj_= null;
            }

            // Attempt to send the datagram to the correct queue
            int loop = 0;
            long waitTimeOut = 0;

            while (true) {  
                boolean sent=false;
                loop++;
                try {
                    // try and forward to out queue
                    sent= f.addToOutQueue(dh,blockWaitObj_);

                    if (sent) {
                        // if it is sent
                        fabricDevice_.inSentPacket(dh.datagram);
                    } else {
                        // it was dropped
                        fabricDevice_.inSentPacket(dh.datagram);
                        f.outDroppedPacket(dh.datagram);
                    }
                    break;
                } catch (NoRouteToHostException e) {
                    fabricDevice_.inDroppedPacketNR(dh.datagram);
                    break;
                } catch (InterfaceBlockedException ex) {
                    if (f.outIsBlocking() == false) {
                        fabricDevice_.inDroppedPacket(dh.datagram);
                        break;
                    } else {
                        // Blocking out queue -- wait
                        synchronized (blockWaitObj_) { 
                            try {
                                //Logger.getLogger("log").logln(USR.ERROR, leadin()+"  run WAIT 500");
                                /**
                                 * TODO: work out why this timing is important.
                                 * A number of 20 is too small, and causes multiple
                                 * loop arounds.
                                 * Putting wait() causes lockups.
                                 */
                                long t0 = System.currentTimeMillis();
                                blockWaitObj_.wait(500);
                                waitTimeOut = System.currentTimeMillis() - t0;
                            } catch (InterruptedException e) {

                            }
                        }
                    }
                }
            }


            //if (loop>1) Logger.getLogger("log").logln(USR.ERROR, leadin()+"  looped " + loop + " waitTimeOut = " + waitTimeOut);


        }
    }

    public void stopThread() {
        synchronized (running_) {
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
    }

    String leadin() {
        return "FDInQ "+name_+": ";
    }
}

class OutQueueHandler implements Runnable {
    int queueDiscipline_= 0;
    LinkedBlockingDeque <DatagramHandle> queue_;
    FabricDevice fabricDevice_= null;
    Boolean outRunning_= false;
    Thread outThread_= null;
    String name_= null;
    LinkedBlockingDeque<Object> outWaitQueue_= null;

    OutQueueHandler(int discipline, LinkedBlockingDeque<DatagramHandle> q,
                    LinkedBlockingDeque<Object> w, FabricDevice f) {
        queueDiscipline_= discipline;
        queue_= q;
        fabricDevice_= f;
        outWaitQueue_= w;
        name_= f.getName();
    }


    public void run() {
        outRunning_= true;
        outThread_= Thread.currentThread();
        Object wake= null;
        while (outRunning_ || queue_.size() > 0 ) {
            // WAS if (queue_.remainingCapacity() > 0 && outWaitQueue_ != null) {
            if (queue_.remainingCapacity() > 0 && outWaitQueue_ != null && outWaitQueue_.size() > 0) {
                try {
                    wake= outWaitQueue_.removeFirst();
                    synchronized (wake) {
                        wake.notify();
                    }
                }
                catch (NoSuchElementException e) {
                }
            }
            DatagramHandle dh= null;
            try {
                dh= queue_.take();
            } catch (InterruptedException e) {
                continue;
            }
            if (dh.datagram.TTLReduce() == false) {
                fabricDevice_.listener_.TTLDrop(dh.datagram);
                fabricDevice_.inDroppedPacketNR(dh.datagram);
                continue;
            }
            while (dh != null) {
                try {
                    boolean sent= fabricDevice_.sendOutDatagram(dh);
                    break;
                } catch (InterfaceBlockedException e) {
                    try {
                        //Logger.getLogger("log").logln(USR.ERROR, leadin()+" OutQueueHandler run WAIT 500");
                        wait(500);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
    }

    public void stopThread() {
        synchronized (outRunning_) {
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

