package usr.router;

import usr.net.*;
import usr.logging.*;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

/** A fabric device is a device with an Inbound and an outbound queue.
   Inboud is "towards fabric" and Outbound is "from fabric" -- see diagram
   Different queueing disciplines can be chosen for incoming and outgoing
   queues
 */
public class FabricDevice implements FabricDeviceInterface {

    String name_ = "Unnamed Fabric Device";  // Device name
    NetIFListener listener_;  //  NetIF listener for this device
    Thread inThread_ = null;  // Thread for incoming queue
    Thread outThread_ = null;  // Thread for outgoing queue
    boolean stopped_ = true;

    static final int QUEUE_BLOCKING = 1;  // If queue is over size then queue wait objects and block
    static final int QUEUE_DROPPING = 2;  // If queue is over size then drop packet
    static final int QUEUE_NOQUEUE = 3;
    int inQueueLen_ = 0;  // Zero will be interpreted as an no limit queue
    int outQueueLen_ = 0;
    int inQueueDiscipline_ = QUEUE_NOQUEUE;
    int outQueueDiscipline_ = QUEUE_NOQUEUE;

    int maxInQueue_ = 0;
    int maxOutQueue_ = 0;

    BlockingDeque<DatagramHandle> inQueue_ = null;  // queue for inbound -- towards fabric
    BlockingDeque<DatagramHandle> outQueue_ = null;  // queue for outgoing -- away from fabric
    DatagramDevice device_;
    InQueueHandler inQueueHandler_ = null;
    OutQueueHandler outQueueHandler_ = null;
    // counts
    NetStats netStats_ = null;

    BlockingDeque<Waker> inWaitQueue_ = null;  // Queue stores objects waiting for
    BlockingDeque<Waker> outWaitQueue_ = null;  // notification from blocking queue

    int inq = 0;
    boolean started_ = false;

    /** Default fabric device has no queue */
    public FabricDevice (DatagramDevice ep, NetIFListener l) {
        device_ = ep;
        listener_ = l;
        netStats_ = new NetStats();
    }

    /** Set the queue type for the inbound queue */
    public void setInQueueDiscipline(int discipline) {
        if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " cannot change queue after fabric start");
            return;
        }

        if (discipline == QUEUE_BLOCKING ||
            discipline == QUEUE_DROPPING ||
            discipline == QUEUE_NOQUEUE) {
            inQueueDiscipline_ = discipline;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " illegal queue discipline for in queue");
        }
    }

    /** Set the queue type for the outbound queue */
    public void setOutQueueDiscipline(int discipline) {
        if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " cannot change queue after fabric start");
            return;
        }

        if (discipline == QUEUE_BLOCKING ||
            discipline == QUEUE_DROPPING ||
            discipline == QUEUE_NOQUEUE) {
            outQueueDiscipline_ = discipline;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " illegal queue discipline for out queue");
        }

    }

    /** Set the queue length for the inbound queue */
    public void setInQueueLength(int len) {
        if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " cannot change queue after fabric start");
            return;
        }

        if (len >= 0) {
            inQueueLen_ = len;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " illegal queue len");
            return;
        }
    }

    /** Set the queue length for the outbound queue */
    public void setOutQueueLength(int len) {
        if (stopped_ == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " cannot change queue after fabric start");
            return;
        }

        if (len >= 0) {
            outQueueLen_ = len;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          " illegal queue len");
            return;
        }

    }

    /*

       /** Start inqueue and out queue threads if necessary */
    public synchronized void start() {
        stopped_ = false;

        if (inQueueDiscipline_ != QUEUE_NOQUEUE) {

            if (inQueueLen_ == 0) {
                inQueue_ = new LinkedBlockingDeque<DatagramHandle>();
            } else {
                inQueue_ = new LinkedBlockingDeque<DatagramHandle>(inQueueLen_);
            }
            inWaitQueue_ = new LinkedBlockingDeque<Waker>();
            inQueueHandler_ = new InQueueHandler(inQueueDiscipline_, inQueue_, this);

            inThread_ = new Thread(inQueueHandler_, "/" + listener_.getName() + "/" + name_+"/InQueue");
            inThread_.start();
        }

        if (outQueueDiscipline_ != QUEUE_NOQUEUE) {
            if (outQueueLen_ == 0) {
                outQueue_ = new LinkedBlockingDeque<DatagramHandle>();
            } else {
                outQueue_ = new LinkedBlockingDeque<DatagramHandle>(outQueueLen_);
            }
            outWaitQueue_ = new LinkedBlockingDeque<Waker>();
            outQueueHandler_ = new OutQueueHandler(outQueueDiscipline_, outQueue_, this);
            outThread_ = new Thread(outQueueHandler_, "/" + listener_.getName() + "/" + name_+"/OutQueue");
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
    void inDroppedPacket(Datagram dg) {

        netStats_.increment(NetStats.Stat.InDropped);

        if (inIsBlocking()) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" in dropped packet");
        }
    }

    /** Register stats using these functions  --- packet dropped but in expected manner*/
    void inDroppedPacketNR(Datagram dg) {
        netStats_.increment(NetStats.Stat.InDropped);

    }

    void outDroppedPacket(Datagram dg) {
        netStats_.increment(NetStats.Stat.OutDropped);
        //  This can happen on blocking out interface if interface is shut beforehand

    }

    void inSentPacket(Datagram dg) {
        // stats
        netStats_.increment(NetStats.Stat.InPackets);
        netStats_.add(NetStats.Stat.InBytes, dg.getTotalLength());

        if (dg.getProtocol() == Protocol.DATA) {
            netStats_.increment(NetStats.Stat.InDataPackets);
            netStats_.add(NetStats.Stat.InDataBytes, dg.getTotalLength());
        }
    }

    void outSentPacket(Datagram dg) {
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

        Waker waker = new Waker();
        synchronized (waker) {
            while (true) {

                try {
                    boolean processed = addToInQueue(dg, dd, waker);
                    return processed;
                } catch (usr.net.InterfaceBlockedException e) {
                    //Logger.getLogger("log").logln(USR.ERROR, leadin() + "InterfaceBlockedException for blockingAddToInQueue " + dg);

                    waker.await(250);
                }
            }
        }
    }

    /** Returns true if datagram is sent or false if dropped */
    public boolean addToInQueue(Datagram dg, DatagramDevice dd)
    throws NoRouteToHostException {
        try {
            return addToInQueue(dg, dd, null);
        } catch (usr.net.InterfaceBlockedException e) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+

                                          " Interface Blocked Exception should not be thrown in addToInQueue");
        }

        return false; // This line should never be reached.
    }

    /** Add a datagram to the in queue -- true means sent to out queue, false means blocked */
    public boolean addToInQueue(Datagram dg, DatagramDevice dd, Waker waitObj)
    throws NoRouteToHostException, usr.net.InterfaceBlockedException {

        DatagramHandle dh = new DatagramHandle(dg, dd);

        if (inQueueDiscipline_ == QUEUE_NOQUEUE) {
            boolean processed = transferDatagram(dh);

            if (processed) {
                inSentPacket(dg);
                return true;
            }

            if (inIsBlocking() && waitObj != null) {
                throw new usr.net.InterfaceBlockedException();
            }

            Logger.getLogger("log").logln(USR.ERROR, leadin() + " inDroppedPacket: addToInQueue() inQueueDiscipline_ ==  QUEUE_NOQUEUE and waitObj == null for " + dh.datagram);

            inDroppedPacket(dg);

            return false;
        } else {
            // QUEUE_BLOCKING || QUEUE_DROPPING

            // check route to host

            // try and get device
            try {
                FabricDevice f = getRouteFabric(dg);
            } catch (NoRouteToHostException nrhe) {
                inDroppedPacketNR(dg);
                throw nrhe;
            }


            // Queue the packet if possible
            synchronized (inQueue_) {
                if (inQueueLen_ == 0 || inQueue_.size() < inQueueLen_) {
                    inQueue_.offerLast(dh);

                    if (inQueue_.size() > maxInQueue_) {
                        maxInQueue_ = inQueue_.size();
                        netStats_.setValue(NetStats.Stat.BiggestInQueue, maxInQueue_);
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
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " inDroppedPacket: addToInQueue() inQueueDiscipline_ ==  QUEUE_BLOCKING || QUEUE_DROPPING and waitObj == null for " + dh.datagram);
                inDroppedPacket(dg);
                return true;
            }
        }
    }

    /**
     * A callback from the InQueueHandler to say
     * that the queue has some space.
     */
    protected void inQueueHasCapacity() {
        Waker wake = null;

        if (inWaitQueue_ != null && inWaitQueue_.size() > 0) {
            try {
                wake = inWaitQueue_.removeFirst();
                // sclayman 19/9/2011 - I dont think we need the synchronized
                //synchronized (wake) {
                wake.signal();
                //}
            } catch (NoSuchElementException e) {
            }
        }
    }

    public NetStats getNetStats() {
        return (NetStats)netStats_.clone();
    }

    /** Set the listener device for this fabric device */
    public void setListener(NetIFListener l) {
        listener_ = l;
    }

    /** Get the listener device */
    public NetIFListener getListener() {
        return listener_;
    }

    /** Add a datagram to the out queue -- blocking call, will continue to wait until
        datagram is added */
    public boolean blockingAddToOutQueue(DatagramHandle dh)
    throws NoRouteToHostException {

        Waker waker = new Waker();

        synchronized (waker) {
            while (true) {

                if (Thread.currentThread().isInterrupted()) {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin()+"blockingAddToOutQueue() isInterrupted");
                    return false;
                }

                try {
                    boolean processed = addToOutQueue(dh, waker);
                    return processed;
                } catch (usr.net.InterfaceBlockedException e) {
                    //Logger.getLogger("log").logln(USR.ERROR, leadin() + "InterfaceBlockedException for blockingAddToOutQueue:  "  + dh.datagram);
                    waker.await(250);
                }
            }
        }
    }

    /** Add a datagram to the out queue -- return true if datagram
       added to out queue, false means rejected*/
    public boolean addToOutQueue(DatagramHandle dh) throws
    NoRouteToHostException, InterfaceBlockedException {
        return addToOutQueue(dh, null);
    }

    /** Add a datagram to the out queue --
       true means added to out queue, false means rejected*/
    public boolean addToOutQueue(DatagramHandle dh, Waker waitObj)
    throws NoRouteToHostException, InterfaceBlockedException {

        if (outQueueDiscipline_ == QUEUE_NOQUEUE) {
            if (dh.datagram.TTLReduce() == false) {
                listener_.TTLDrop(dh.datagram);
                return false;
            }
            boolean processed = sendOutDatagram(dh);
            return true;

        } else {
            // QUEUE_BLOCKING || QUEUE_DROPPING

            // Queue the packet if possible
            synchronized (outQueue_) {
                if (outQueueLen_ == 0 || outQueue_.size() < outQueueLen_) {
                    outQueue_.offerLast(dh);

                    if (outQueue_.size() > maxOutQueue_) {
                        maxOutQueue_ = outQueue_.size();
                        netStats_.setValue(NetStats.Stat.BiggestOutQueue, maxOutQueue_);
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

    /**
     * A callback from the OutQueueHandler to say
     * that the queue has some space.
     */
    protected void outQueueHasCapacity() {
        Waker wake = null;

        if (outWaitQueue_ != null && outWaitQueue_.size() > 0) {
            try {
                wake = outWaitQueue_.removeFirst();
                // sclayman 19/9/2011 - I dont think we need the synchronized
                //synchronized (wake) {
                wake.signal();
                //}
            } catch (NoSuchElementException e) {
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
        FabricDevice f = null;
        try {
            f = getRouteFabric(dh.datagram);
        } catch (NoRouteToHostException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "NoRouteToHostException for transferDatagram " + dh.datagram);
            throw (e);
        }

        Waker waker = new Waker();

        while (true) {
            try {
                return f.addToOutQueue(dh, waker);
            } catch (java.net.NoRouteToHostException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "NoRouteToHostException for transferDatagram " + dh.datagram);
                return false;
            } catch (usr.net.InterfaceBlockedException ex) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "InterfaceBlockedException for transferDatagram " + dh.datagram);

                synchronized (waker) {
                    waker.await(250); 
                }
            }
        }
    }

    /** Send the outbound Datagram onwards */
    public boolean sendOutDatagram(DatagramHandle dh) throws InterfaceBlockedException  {
        Datagram dg = dh.datagram;
        DatagramDevice dd = dh.datagramDevice;

        boolean sent = device_.outQueueHandler(dh.datagram, dh.datagramDevice);

        if (sent) {
            outSentPacket(dh.datagram);
        } else {
            outDroppedPacket(dh.datagram);
        }
        return sent;
    }

    /** Stop any running threads */
    public void stop() {

        stopped_ = true;

        if (inQueueHandler_ != null) {
            inQueueHandler_.stopThread();
        }

        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" in queue stop");
        if (outQueueHandler_ != null) {
            outQueueHandler_.stopThread();
        }

        // wake up all inWaitQueue_ and outWaitQueue_
        // so they do not lock
        while (inWaitQueue_ != null && inWaitQueue_.size() > 0) {
            inQueueHasCapacity();
        }

        while (outWaitQueue_ != null && outWaitQueue_.size() > 0) {
            outQueueHasCapacity();
        }

        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" out queue stop");
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+" fabric device stop");

    }

    void setName(String name) {
        name_ = name;

        if (inThread_ != null) {
            inThread_.setName("/" + listener_.getName() + "/" + name+"/InQueue");
        }

        if (outThread_ != null) {
            outThread_.setName("/" + listener_.getName() + "/" + name+"/OutQueue");
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

    int queueDiscipline_ = 0;
    BlockingQueue<DatagramHandle> queue_ = null;
    FabricDevice fabricDevice_ = null;
    Boolean running_ = false;
    Thread runThread_ = null;
    CountDownLatch latch = null;
    String name_;

    /** Constructor sets up */
    InQueueHandler(int discipline, BlockingQueue<DatagramHandle> q, FabricDevice f) {
        queueDiscipline_ = discipline;
        queue_ = q;
        fabricDevice_ = f;
        name_ = f.getName();
        latch = new CountDownLatch(1);
    }

    public void run() {
        running_ = true;
        runThread_ = Thread.currentThread();
        int ct = 0;

        while (running_ || queue_.size() > 0) {
            // Consider waking next in line if we're a blocking queue
            if (queue_.remainingCapacity() > 0) {
                fabricDevice_.inQueueHasCapacity();
            }

            // Get a datagram from the queue
            DatagramHandle dh = null;

            try {
                dh = queue_.take();
            } catch (InterruptedException e) {
                break;  // Interrupt should only occur when queue is zero
            }

            // Process DatagramHandle
            FabricDevice f = null;
            try {
                f = fabricDevice_.getRouteFabric(dh.datagram);
            } catch (NoRouteToHostException e) { //  Cannot route
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " inDroppedPacketNR: InQueueHandler run() NoRouteToHostException for " + dh.datagram);

                fabricDevice_.inDroppedPacketNR(dh.datagram);
                //Logger.getLogger("log").logln(USR.ERROR, leadin()+"Cannot route " + dh.datagram);
                continue;
            }

            // Attempt to send the datagram to the correct queue
            int loop = 0;

            while (true) {
                boolean sent = false;
                loop++;
                try {
                    // try and forward to out queue
                    if (f.outIsBlocking()) {
                        sent = f.blockingAddToOutQueue(dh);
                    } else {
                        sent = f.addToOutQueue(dh);
                    }

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
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "InQueueHandler run NoRouteToHostException");
                    fabricDevice_.inDroppedPacketNR(dh.datagram);
                    break;
                } catch (InterfaceBlockedException ex) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "InQueueHandler run InterfaceBlockedException");

                    if (f.outIsBlocking() == false) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + " inDroppedPacketNR: InQueueHandler InterfaceBlockedException run() for " + dh.datagram);
                        fabricDevice_.inDroppedPacket(dh.datagram);
                        break;
                    } else {

                    }
                }
            }

        }

        // reduce latch count by 1
        latch.countDown();

    }

    public void stopThread() {
        synchronized (running_) {
            running_ = false;

            if (queue_.size() == 0) {
                runThread_.interrupt();
            }

            // join myThread when it is finished
            try {
                latch.await();
            } catch (InterruptedException ie) {
            }
  
            /*
             * try {
             * runThread_.join();
             * } catch (InterruptedException e) {
             * }
             */

            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"thread stopped");
        }
    }

    String leadin() {
        return "FDInQ "+name_+": ";
    }

}

class OutQueueHandler implements Runnable {
    int queueDiscipline_ = 0;
    FabricDevice fabricDevice_ = null;
    Boolean outRunning_ = false;
    Thread outThread_ = null;
    CountDownLatch latch = null;
    String name_ = null;
    BlockingQueue<DatagramHandle> queue_;

    OutQueueHandler(int discipline, BlockingQueue<DatagramHandle> q, FabricDevice f) {
        queueDiscipline_ = discipline;
        queue_ = q;
        fabricDevice_ = f;
        name_ = f.getName();
        latch = new CountDownLatch(1);
    }

    public void run() {
        outRunning_ = true;
        outThread_ = Thread.currentThread();

        while (outRunning_ || queue_.size() > 0 ) {

            if (queue_.remainingCapacity() > 0) {
                fabricDevice_.outQueueHasCapacity();
            }

            DatagramHandle dh = null;
            try {
                dh = queue_.take();
            } catch (InterruptedException e) {
                continue;
            }

            // process DatagramHandle
            if (dh.datagram.TTLReduce() == false) {
                fabricDevice_.listener_.TTLDrop(dh.datagram);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " inDroppedPacketNR: OutQueueHandler run() for " + dh.datagram);
                fabricDevice_.inDroppedPacketNR(dh.datagram);
                continue;
            }

            while (dh != null) {
                try {
                    boolean sent = fabricDevice_.sendOutDatagram(dh);
                    break;
                } catch (InterfaceBlockedException e) {
                    try {
                        Logger.getLogger("log").logln(USR.ERROR, leadin()+" OutQueueHandler run WAIT 500");
                        wait(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "OutQueueHandler run wait Interrupted");
                    }
                }
            }
        }

        // reduce latch count by 1
        latch.countDown();


    }

    public void stopThread() {
        synchronized (outRunning_) {
            if (outRunning_ == false) {
                return;
            }

            outRunning_ = false;

            if (queue_.size() == 0) {
                outThread_.interrupt();
            }

            // join myThread if it is not finished
            try {
                latch.await();
            } catch (InterruptedException ie) {
            }
  
            /*
             * try {
             *   outThread_.join();
             * } catch (InterruptedException e) {
             *
             *}
             */

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
        datagram = dg;
        datagramDevice = d;
    }

}

/**
 * A Wakeup Monitor.
 */
class Waker {
    int waitCount = 0;
    int notifyCount = 0;
    boolean isWaiting = false;
    long waitTimeOut = 0;

    public Waker() {
    }

    /**
     * Wait for some milliseconds.
     */
    public synchronized void await(long millisTimeout) {
        if (notifyCount > waitCount) {
            //Logger.getLogger("log").logln(USR.ERROR, "Waker " + hashCode() + " notified waitCount: " + waitCount + " notifyCount:
            // " + notifyCount);
            waitCount++;
            return;
        } else {

            if (isWaiting) {
                // already waiting - nothing to do
                //Logger.getLogger("log").logln(USR.ERROR, "Waker " + hashCode() + " already waiting");
                return;
            } else {
                try {
                    isWaiting = true;
                    waitCount++;

                    long t0 = System.currentTimeMillis();

                    //Logger.getLogger("log").logln(USR.ERROR, "Waker " + hashCode() + " wait " + millisTimeout);

                    this.wait(millisTimeout);

                    waitTimeOut = System.currentTimeMillis() - t0;

                } catch (InterruptedException ie) {
                    Logger.getLogger("log").logln(USR.ERROR, "Waker " + hashCode() + " interrupted");
                } finally {
                    isWaiting = false;

                    if (waitTimeOut > 10) {
                        Logger.getLogger("log").logln(USR.ERROR, "Waker " + hashCode() + " waitTimeOut = " + waitTimeOut);
                    }
                }
            }
        }
    }

    /**
     * Signal with a notify
     */
    public synchronized void signal() {
        notifyCount++;

        if (isWaiting) {
            this.notify();

        } else {
            // nothing to do
        }
    }

}
