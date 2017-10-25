package usr.router;


import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import usr.common.TimedThread;
import usr.common.TimedThreadGroup;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.protocol.Protocol;

import usr.net.NetworkException;

/**
 * The InterceptNetIF class acts like a virtual NetIF for intercepting
 * all packets on a NetIF.
 */
public class InterceptNetIF implements NetIF {
    // the RouterController
    RouterController controller;
    boolean theEnd = false;
    // the count of the no of Datagrams
    int datagramCount = 0;

    // The queue to take from
    LinkedBlockingQueue<Datagram> queue;

    int timeout = 0;

    // My Thread
    Thread takeThread;
    boolean running = false;
    Object threadSyncObj = new Object();

    FabricDevice fabricDevice_ = null;
    RouterPort port = null;

    /*
     * NetIF stuff
     */

    // My name
    String name;
    // My address
    Address address;

    // Remote name
    String remoteName;
    // Remote address
    Address remoteAddress;

    // weight
    int weight;
    // ID
    int id;

    // NetIFListener
    NetIFListener listener;

    boolean isClosed = false;

    /**
     * Construct an AppSocketMux.
     */
    InterceptNetIF(NetIF netIF) {
        Router router = RouterDirectory.getRouter();

        this.controller = router.getRouterController();
        name = new String("intercept-" + netIF.getName());

        queue = new LinkedBlockingQueue<Datagram>();
    }

    /**
     * Start me up.
     */
    public boolean start() {
        synchronized (threadSyncObj) {
            try {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

                // start my own thread
                ThreadGroup group = new TimedThreadGroup(name);


                running = true;
                address = AddressFactory.newAddress(0);
                fabricDevice_ = new FabricDevice(group, this, controller.getListener());

                fabricDevice_.setInQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
                fabricDevice_.setInQueueLength(1000);
                fabricDevice_.setName(name);
                fabricDevice_.start();
                boolean connected = connect();
                return connected;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " InterceptNetIF not started");
                e.printStackTrace();
                running = false;
                return false;
            }
        }
    }

    /**
     * Close all sockets.
     */
    public boolean stop() {
        synchronized (threadSyncObj) {
            if (running == true) {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");
                fabricDevice_.stop();
                // stop my own thread
                running = false;

                close();
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stopped");
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Wait for this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void waitFor() {
        try {
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"waiting");
            synchronized (threadSyncObj) {
                setTheEnd();
                wait();
            }
        } catch (InterruptedException ie) {
        }

    }

    /**
     * Notify this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void theEnd() {
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "theEnd");
        while (!ended()) {
            try {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"In a loop");
                Thread.sleep(100);
            } catch (Exception e) {

            }
        }

        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"notifying");
        synchronized (threadSyncObj) {
            threadSyncObj.notify();
        }

    }

    synchronized void setTheEnd() {
        theEnd = true;
    }

    synchronized boolean ended() {
        return theEnd;
    }

    private void pause() {
        synchronized (threadSyncObj) {
            try {
                threadSyncObj.wait();
            } catch (InterruptedException ie) {
            }
        }
    }

    /**
     * Connect to my local Router.
     */
    @Override
    public boolean connectPhase1() throws IOException {
        return connect();
    }

    /**
     * Connect to my local Router.
     */
    @Override
    public boolean connectPhase2() throws IOException {
        return true;
    }

    private boolean connect() throws IOException {
        setID(0);
        setName(name);
        setWeight(0);

        // DON'T plug it in to the Router Fabric
        ////controller.registerTemporaryNetIF(this);
        ////controller.plugTemporaryNetIFIntoPort(this);

        return true;
    }

    /** The fabric device which moves packets to/from this interface */
    @Override
    public FabricDevice getFabricDevice() {
        return fabricDevice_;
    }

    /**
     * Get the name of this NetIF.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the name of this NetIF.
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the ID of this NetIF.
     */
    @Override
    public int getID() {
        return id;
    }

    /**
     * Set the ID of this NetIF.
     */
    @Override
    public void setID(int id) {
        this.id = id;
    }

    /**
     * Get the weight of this NetIF.
     */
    @Override
    public int getWeight() {
        return weight;
    }

    /**
     * Set the weight of this NetIF.
     */
    @Override
    public void setWeight(int w) {
        weight = w;
    }

    /**
     * Get the Address for this connection.
     */
    @Override
    public Address getAddress() {
        // return controller.getAddress();
        return address;
    }

    /**
     * Set the Address for this connection.
     */
    @Override
    public void setAddress(Address addr) {
        System.err.println("DO NOT SET ADDRESS FOR ASM");
        System.exit(-1);
    }

    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    @Override
    public String getRemoteRouterName() {
        return controller.getName();
    }

    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    @Override
    public void setRemoteRouterName(String name) {
        System.err.println("Do not call setRemoteRouterName for InterceptNetIF");
        System.exit(-1);
    }

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    @Override
    public Address getRemoteRouterAddress() {
        return controller.getAddress();
    }

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    @Override
    public void setRemoteRouterAddress(Address addr) {
        System.err.println("Do not call setRemoteRouterAddress for InterceptNetIF");
        System.exit(-1);
    }

    /**
     * Get the interface stats.
     * Returns a NetStats object.
     */
    @Override
    public NetStats getStats() {
        return fabricDevice_.getNetStats();
    }

    /**
     * Close a NetIF
     */
    @Override
    public void close() {
        if (!isClosed()) {
            isClosed = true;

            if (takeThread != null) {
                takeThread.interrupt();
            }
        }
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * Is closed.
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Get the Listener of a NetIF.
     */
    @Override
    public NetIFListener getNetIFListener() {
        return listener;
    }

    /**
     * Set the Listener of NetIF.
     */
    @Override
    public void setNetIFListener(NetIFListener l) {
        listener = l;
        fabricDevice_.setListener(l);

    }

    /**
     * Get the RouterPort a NetIF is plugIged into.
     */
    @Override
    public RouterPort getRouterPort() {
        return port;
    }

    /**
     * Set the RouterPort a NetIF is plugIged into.
     */
    @Override
    public void setRouterPort(RouterPort rp) {
        port = rp;
    }

    /** Remote close received */
    @Override
    public void remoteClose() {
        close();
    }

    /**
     * Send a Datagram -- sets source to this interface and puts the datagram
       on the incoming queue for this interface
     */
    @Override
    public boolean sendDatagram(Datagram dg) throws NoRouteToHostException {
        dg.setSrcAddress(address);
        if (running == true) {
            return enqueueDatagram(dg);
        } else {
            return false;
        }
    }

    /**
     * Puts a datagram on the incoming queue for this network interface
     */
    @Override
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException {
        return fabricDevice_.blockingAddToInQueue(dg, this);
    }

    /**
     *   Deliver a received datagram to the queue 
     */
    @Override
    public synchronized boolean recvDatagramFromDevice(Datagram datagram, DatagramDevice device) {
        // if (running == false)  // If we're not running simply pretend to have received it
        //     return true;
        if (datagram.getProtocol() == Protocol.CONTROL) {
            datagramCount++;
            byte[] payload = datagram.getPayload();
            byte controlChar = payload[0];

            if (controlChar == 'C') {
                remoteClose();
            }
            return true;
        }


        if (isClosed) {
            // it's closed so no need to copy
            return false;
        } else {
            queue.add(datagram);

            return true;
        }
    }

    public Datagram receive() throws NetworkException {
        if (isClosed) {
            throw new NetworkException("DatagramCapture closed");
        }

        takeThread = Thread.currentThread();
        try {
            if (timeout == 0) {
                return queue.take();
            } else {
                Datagram obj = queue.poll(timeout, TimeUnit.MILLISECONDS);

                if (obj != null) {
                    return obj;
                } else {
                    throw new NetworkException("timeout: " + timeout);
                }
            }
        } catch (InterruptedException ie) {
            if (isClosed) {
                Logger.getLogger("log").logln(USR.STDOUT, "DatagramCapture closed on shutdown");
                return null;
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "DatagramCapture receive interrupted");
                throw new NetworkException("DatagramCapture receive interrupted");
            }
        }

    }



    /**
     * Get the remote address to which this socket is connected.
     */
    public InetAddress getInetAddress() {
        throw new UnsupportedOperationException("no underyling socket");
    }

    /**
     * Gets the local address to which the socket is bound.
     */
    public InetAddress getLocalAddress() {
        throw new UnsupportedOperationException("no underyling socket");
    }


    /**
     * Get the remote port number to which this socket is connected.
     */
    public int getPort() {
        throw new UnsupportedOperationException("no underyling socket");
    }


    /**
     * Get the port number on the local host to which this socket is bound.
     */
    public int getLocalPort() {
        throw new UnsupportedOperationException("no underyling socket");
    }


    /**
     * Set the remote InetAddress and port
     */
    public void setRemoteAddress(InetAddress addr, int port) {
        throw new UnsupportedOperationException("no underyling socket");
    }


    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String AS = "InterceptNetIF: ";

        return controller.getName() + " " + AS;
    }

}
