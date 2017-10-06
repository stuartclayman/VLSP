package usr.router;

import java.net.NoRouteToHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import usr.common.TimedThread;
import usr.common.TimedThreadGroup;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.InterfaceBlockedException;
import usr.protocol.Protocol;

/** A fabric device is a device with an Inbound and an outbound queue.
    Inboud is "towards fabric" and Outbound is "from fabric" -- see diagram
    Different queueing disciplines can be chosen for incoming and outgoing
    queues
*/
public class FabricDevice implements FabricDeviceInterface {

    String name_ = "Unnamed Fabric Device";  // Device name
    NetIFListener listener_;  //  NetIF listener for this device - the RouterFabric.
    Thread inThread_ = null;  // Thread for incoming queue
    Thread outThread_ = null;  // Thread for outgoing queue
    ThreadGroup group = null;
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

    BlockingDeque<DatagramHandle> inboundQueue_ = null;  // queue for inbound -- towards fabric
    BlockingDeque<DatagramHandle> outboundQueue_ = null;  // queue for outgoing -- away from fabric
    Object inQueueSyncObj_ = new Object();
    Object outQueueSyncObj_ = new Object();
    
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

    /** Default fabric device has no queue */
    public FabricDevice (ThreadGroup group, DatagramDevice ep, NetIFListener l) {
        device_ = ep;
        listener_ = l;
        netStats_ = new NetStats();
        this.group = group;
    }

    /** Start inqueue and out queue threads if necessary */
    @Override
    public synchronized void start() {
        stopped_ = false;

        if (group == null) {
            group = new TimedThreadGroup(getName());
        }


        if (inQueueDiscipline_ != QUEUE_NOQUEUE) {

            if (inQueueLen_ == 0) {
                inboundQueue_ = new LinkedBlockingDeque<DatagramHandle>();
            } else {
                inboundQueue_ = new LinkedBlockingDeque<DatagramHandle>(inQueueLen_);
            }
            inWaitQueue_ = new LinkedBlockingDeque<Waker>();
            inQueueHandler_ = new InQueueHandler(inQueueDiscipline_, inboundQueue_, this);

            inThread_ = new TimedThread(group, inQueueHandler_, "/" + name_+"/InQueue");
            inThread_.start();
        }

        if (outQueueDiscipline_ != QUEUE_NOQUEUE) {
            if (outQueueLen_ == 0) {
                outboundQueue_ = new LinkedBlockingDeque<DatagramHandle>();
            } else {
                outboundQueue_ = new LinkedBlockingDeque<DatagramHandle>(outQueueLen_);
            }
            outWaitQueue_ = new LinkedBlockingDeque<Waker>();
            outQueueHandler_ = new OutQueueHandler(outQueueDiscipline_, outboundQueue_, this);
            outThread_ = new TimedThread(group, outQueueHandler_, "/" + name_+"/OutQueue");
            outThread_.start();
        }
    }

    /**
     * Set a Datagram Intercepter
     */
    public FabricDevice setDatagramIntercepter(NetIF netif) {
        inQueueHandler_.setDatagramIntercepter(netif);

        return this;
    }

    /**
     * Add a DatagramCapture listener
     */
    public FabricDevice addDatagramCaptureListener(DatagramCapture dcap) {
        inQueueHandler_.addDatagramCaptureListener(dcap);

        return this;
    }

    /**
     * Remove a DatagramCapture listener
     */
    public FabricDevice removeDatagramCaptureListener(DatagramCapture dcap) {
        inQueueHandler_.removeDatagramCaptureListener(dcap);

        return this;
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

    /** 
     * Callback for when InQueueHandler sent a Datagram onwards
     */
    void inSentPacket(Datagram dg) {
        // stats
        netStats_.increment(NetStats.Stat.InPackets);
        netStats_.add(NetStats.Stat.InBytes, dg.getTotalLength());

        if (dg.getProtocol() == Protocol.DATA) {
            netStats_.increment(NetStats.Stat.InDataPackets);
            netStats_.add(NetStats.Stat.InDataBytes, dg.getTotalLength());
        }
    }

    /** 
     * Callback for when OutQueueHandler sent a Datagram onwards
     */
    void outSentPacket(Datagram dg) {
        netStats_.increment(NetStats.Stat.OutPackets);
        netStats_.add(NetStats.Stat.OutBytes, dg.getTotalLength());

        if (dg.getProtocol() == Protocol.DATA) {
            netStats_.increment(NetStats.Stat.OutDataPackets);
            netStats_.add(NetStats.Stat.OutDataBytes, dg.getTotalLength());
        }

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

    /** Return the fabric device that this datagram may route to or no
        such fabric */
    FabricDevice lookupRoutingFabricDevice(Datagram dg) throws NoRouteToHostException {
        if (listener_ == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" no listener");
            return null;
        }
        return listener_.lookupRoutingFabricDevice(dg);
    }

    /** Add a datagram to the in queue -- blocking call, will continue to wait until
        datagram is added */
    public boolean blockingAddToInQueue(Datagram dg, DatagramDevice dd)
        throws NoRouteToHostException {

        Waker waker = new Waker();
        synchronized (waker) {
            while (!stopped_) {          // sclayman 20160718 was (true)
                try {
                    boolean processed = addToInQueue(dg, dd, waker);
                    return processed;
                } catch (usr.net.InterfaceBlockedException e) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "InterfaceBlockedException for blockingAddToInQueue " + dg);

                    waker.await(250);
                }
            }
            return false;
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
                throw new usr.net.InterfaceBlockedException("Interface NoQueueing Blocked to " + dg.getDstAddress().asTransmitForm());
            }

            Logger.getLogger("log").logln(USR.ERROR, leadin() + " inDroppedPacket: addToInQueue() inQueueDiscipline_ ==  QUEUE_NOQUEUE and waitObj == null for " + dh.datagram);

            inDroppedPacket(dg);

            return false;
        } else {
            // QUEUE_BLOCKING || QUEUE_DROPPING

            // check route to host

            // try and get device
            try {
                lookupRoutingFabricDevice(dg);
            } catch (NoRouteToHostException nrhe) {
                inDroppedPacketNR(dg);
                throw nrhe;
            }


            // Queue the packet if possible
            
            synchronized (inQueueSyncObj_) {
                if (inQueueLen_ == 0 || inboundQueue_.size() < inQueueLen_) {
                    inboundQueue_.offerLast(dh);

                    if (inboundQueue_.size() > maxInQueue_) {
                        maxInQueue_ = inboundQueue_.size();
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
                throw new usr.net.InterfaceBlockedException("Interface WithBlocking Blocked to " + dg.getDstAddress().asTransmitForm());
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
            while (!stopped_) {          // sclayman 20160718 was (true)

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
            return false;

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
            sendOutDatagram(dh);
            return true;

        } else {
            // QUEUE_BLOCKING || QUEUE_DROPPING

            // Queue the packet if possible

            synchronized (outQueueSyncObj_) {
                if (outQueueLen_ == 0 || outboundQueue_.size() < outQueueLen_) {
                    outboundQueue_.offerLast(dh);

                    if (outboundQueue_.size() > maxOutQueue_) {
                        maxOutQueue_ = outboundQueue_.size();
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
                throw new InterfaceBlockedException("Interface WithBlocking Blocked to " + dh.datagram.getDstAddress().asTransmitForm());
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
            f = lookupRoutingFabricDevice(dh.datagram);
        } catch (NoRouteToHostException e) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "NoRouteToHostException for transferDatagram " + dh.datagram);
            throw (e);
        }

        Waker waker = new Waker();

        while (!stopped_) {          // sclayman 20160718 was (true)
            try {
                return f.addToOutQueue(dh, waker);
            } catch (java.net.NoRouteToHostException e) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "NoRouteToHostException for transferDatagram " + dh.datagram);
                return false;
            } catch (usr.net.InterfaceBlockedException ex) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "InterfaceBlockedException " + name_ + " for transferDatagram " + dh.datagram);

                synchronized (waker) {
                    waker.await(250);
                }
            }
        }
        return false;

    }

    /** Send the outbound Datagram onwards */
    public boolean sendOutDatagram(DatagramHandle dh) throws InterfaceBlockedException  {
        boolean sent = device_.recvDatagramFromDevice(dh.datagram, dh.datagramDevice);

        if (sent) {
            outSentPacket(dh.datagram);
        } else {
            outDroppedPacket(dh.datagram);
        }
        return sent;
    }

    /** Stop any running threads */
    @Override
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
    }

    void setName(String name) {
        name_ = name;

        if (inThread_ != null) {
            inThread_.setName("/" + name+"/InQueue");
        }

        if (outThread_ != null) {
            outThread_.setName("/" + name+"/OutQueue");
        }

    }

    String getName() {
        return name_;
    }

    String leadin() {
        return "FD: "+name_+": ";
    }

}

/**
 * The InQueueHandler queues packets inbound and sends them onto the fabric.
 */
class InQueueHandler implements Runnable {

    int queueDiscipline_ = 0;
    BlockingQueue<DatagramHandle> queue_ = null;
    FabricDevice fabricDevice_ = null;
    boolean running_ = false;
    Thread runThread_ = null;
    CountDownLatch latch = null;
    String name_;
    Object threadSyncObj_ = new Object();

    // Datagram Intercepter
    NetIF intercepter = null;

    // DatagramCapture listeners
    CopyOnWriteArrayList<DatagramCapture> capture = null;


    /** Constructor sets up */
    InQueueHandler(int discipline, BlockingQueue<DatagramHandle> q, FabricDevice f) {
        queueDiscipline_ = discipline;
        queue_ = q;
        fabricDevice_ = f;
        name_ = f.getName();
        latch = new CountDownLatch(1);
    }

    @Override
    public void run() {
        running_ = true;
        runThread_ = Thread.currentThread();
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
            FabricDevice forwardDevice = null;
            try {
                // find out what FabricDevice to forward this datagram to
                if (intercepter != null) {
                    forwardDevice = intercepter.getFabricDevice();
                } else {
                    forwardDevice = fabricDevice_.lookupRoutingFabricDevice(dh.datagram);
                }

            } catch (NoRouteToHostException e) { //  Cannot route
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + " inDroppedPacketNR: InQueueHandler run() NoRouteToHostException for " + dh.datagram);

                fabricDevice_.inDroppedPacketNR(dh.datagram);
                //Logger.getLogger("log").logln(USR.ERROR, leadin()+"Cannot route " + dh.datagram);
                continue;
            }

            while (running_) {          // sclayman 20160718 was (true)
                boolean sent = false;
                try {
                    // try and forward to out queue
                    if (forwardDevice.outIsBlocking()) {
                        sent = forwardDevice.blockingAddToOutQueue(dh);
                    } else {
                        sent = forwardDevice.addToOutQueue(dh);
                    }

                    if (sent) {
                        // if it is sent
                        fabricDevice_.inSentPacket(dh.datagram);

                        //  possibly pass the datagram onto some PCAP style listeners
                        if (capture != null && capture.size() > 0) {

                            // copy datagrams to all capture listeners
                            try {
                                Datagram newDG = (Datagram)dh.datagram.clone();


                                for (DatagramCapture dcap : capture) {
                                    dcap.sendDatagram(newDG);
                                }
                            } catch (CloneNotSupportedException e) {
                            }
                        }

                    } else {
                        // it was dropped
                        fabricDevice_.inSentPacket(dh.datagram);
                        forwardDevice.outDroppedPacket(dh.datagram);
                    }
                    break;
                } catch (NoRouteToHostException e) {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "InQueueHandler run NoRouteToHostException");
                    fabricDevice_.inDroppedPacketNR(dh.datagram);
                    break;
                } catch (InterfaceBlockedException ex) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "InQueueHandler run InterfaceBlockedException");

                    if (forwardDevice.outIsBlocking() == false) {
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

    /**
     * Set a Datagram Intercepter
     */
    public void setDatagramIntercepter(NetIF netif) {
        intercepter = netif;
    }

    /**
     * Add a DatagramCapture listener
     */
    public void addDatagramCaptureListener(DatagramCapture dcap) {
        if (capture == null) {
            capture = new CopyOnWriteArrayList<DatagramCapture>();
        }

        capture.addIfAbsent(dcap);
    }

    /**
     * Remove a DatagramCapture listener
     */
    public void removeDatagramCaptureListener(DatagramCapture dcap) {
        if (capture == null) {
            return;
        }

        capture.remove(dcap);
    }

    public void stopThread() {
        synchronized (threadSyncObj_) {
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
    boolean outRunning_ = false;
    Thread outThread_ = null;
    CountDownLatch latch = null;
    String name_ = null;
    BlockingQueue<DatagramHandle> queue_;
    Object threadSyncObj_ = new Object();

    OutQueueHandler(int discipline, BlockingQueue<DatagramHandle> q, FabricDevice f) {
        queueDiscipline_ = discipline;
        queue_ = q;
        fabricDevice_ = f;
        name_ = f.getName();
        latch = new CountDownLatch(1);
    }

    @Override
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

            // TTL is reduced to zero - so drop
            if (dh != null && dh.datagram.TTLReduce() == false) {
                fabricDevice_.listener_.TTLDrop(dh.datagram);
                // Logger.getLogger("log").logln(USR.ERROR, leadin() + " Dropped Packet: OutQueueHandler run() for " + dh.datagram);
                fabricDevice_.inDroppedPacketNR(dh.datagram);
                continue;
            }

            // send out
            while (dh != null) {
                try {
                    fabricDevice_.sendOutDatagram(dh);
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
        synchronized (threadSyncObj_) {
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
    final Object syncObject = new Object();

    public Waker() {
    }

    /**
     * Wait for some milliseconds.
     */
    public void await(long millisTimeout) {
        synchronized (syncObject) {
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

                        syncObject.wait(millisTimeout);

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
    }

    /**
     * Signal with a notify
     */
    public void signal() {
        synchronized (syncObject) {
            notifyCount++;

            if (isWaiting) {
                syncObject.notify();

            } else {
                // nothing to do
            }
        }
    }

}
