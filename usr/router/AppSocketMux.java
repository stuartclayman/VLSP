package usr.router;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import usr.common.TimedThread;
import usr.common.TimedThreadGroup;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.protocol.Protocol;

/**
 * The AppSocketMux class allocates pseudo sockets as as application
 * layer function in order that applications can send data to each other
 * and have an address and a port.
 */
public class AppSocketMux implements NetIF {
    // the RouterController
    RouterController controller;
    boolean theEnd = false;
    // the count of the no of Datagrams
    int datagramCount = 0;
    // the next free port
    int freePort = 32768;

    // The list of all AppSockets
    HashMap<Integer, AppSocket> socketMap;

    // The queues of Datagrams for all AppSockets
    HashMap<Integer, LinkedBlockingQueue<Datagram> > socketQueue;

    // My Thread
    Thread myThread;
    boolean running = false;
    boolean waiting = false;
    Object threadSyncObj = new Object();

    boolean removeRequested_ = false;

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

    // stats for each socket
    HashMap<Integer, NetStats> socketStats;

    /**
     * Construct an AppSocketMux.
     */
    AppSocketMux(RouterController controller) {
        this.controller = controller;
        name = new String("localnet");
        socketMap = new HashMap<Integer, AppSocket>();
        socketQueue = new HashMap<Integer, LinkedBlockingQueue<Datagram> >();
        socketStats = new HashMap<Integer, NetStats>();
    }

    /**
     * Start me up.
     */
    public boolean start() {
        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

            // start my own thread
            ThreadGroup group = new TimedThreadGroup("localnet");


            running = true;
            address = AddressFactory.newAddress(0);
            fabricDevice_ = new FabricDevice(group, this, controller.getListener());

            fabricDevice_.setInQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
            fabricDevice_.setInQueueLength(1000);
            fabricDevice_.setName("localnet");
            fabricDevice_.start();
            boolean connected = connect();
            return connected;
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " AppSocketMux not started");
            e.printStackTrace();
            running = false;
            return false;
        }
    }

    /**
     * Close all sockets.
     */
    public boolean stop() {
        synchronized (threadSyncObj) {
            if (running == true) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");
                fabricDevice_.stop();
                // stop my own thread
                running = false;

                HashSet<AppSocket> sockets = new HashSet<AppSocket>(socketMap.values());

                for (AppSocket s : sockets) {
                    s.close();
                }

                close();
                // Logger.getLogger("log").logln(USR.STDOUT, leadin() + "reached end of stop");
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
            synchronized (this) {
                setTheEnd();
                waiting = true;
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
        if (waiting) {
            synchronized (this) {
                notify();
            }
        }

    }

    synchronized void setTheEnd() {
        theEnd = true;
    }

    synchronized boolean ended() {
        return theEnd;
    }

    private synchronized void pause() {
        try {
            waiting = true;
            wait();
            waiting = false;
        } catch (InterruptedException ie) {
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
        setName("localnet");
        setWeight(0);

        // now plug it in to the Router Fabric
        controller.registerTemporaryNetIF(this);
        controller.plugTemporaryNetIFIntoPort(this);

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
        System.err.println("Do not call setRemoteRouterName for ASM");
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
        System.err.println("Do not call setRemoteRouterAddress for ASM");
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
     * Get the socket stats.
     * Returns a NetStats object for each socket, by port number
     */
    public Map<Integer, NetStats> getSocketStats() {
        // now add queues for sockets
        for (int port : socketStats.keySet()) {
            NetStats stats = socketStats.get(port);
        }

        return socketStats;
    }

    /**
     * Close a NetIF
     */
    @Override
    public void close() {
        if (!isClosed()) {
            isClosed = true;
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
        Deliver a received datagram to the appropriate app
     */
    @Override
    public synchronized boolean outQueueHandler(Datagram datagram, DatagramDevice device) {
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
        int dstPort = datagram.getDstPort();

        // find the socket to deliver to
        AppSocket socket = socketMap.get(dstPort);

        if (socket == null) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() +
                                          "Can't deliver to local port " + dstPort + " from " + datagram.getSrcAddress() + "/" +
                                          datagram.getSrcPort());
            return true;  // Returns true as packet is dropped not blocked
        }
        LinkedBlockingQueue<Datagram> portQueue = getQueueForPort(dstPort);
        portQueue.add(datagram);
        NetStats stats = socketStats.get(dstPort);
        stats.increment(NetStats.Stat.InPackets);
        stats.add(NetStats.Stat.InBytes, datagram.getTotalLength());
        return true;
    }

    /**
     * Add an AppSocket.
     */
    synchronized void addAppSocket(AppSocket s) {
        s.localAddress = controller.getAddress();

        int port = s.getLocalPort();

        //  Logger.getLogger("log").logln(USR.ERROR, leadin() + "addAppSocket " + port + "  -> " + s);


        // register the socket
        socketMap.put(port, s);

        // set up the incoming queue
        socketQueue.put(port, new LinkedBlockingQueue<Datagram>());

        // set up the stats
        socketStats.put(port, new NetStats());


        //usr.common.ThreadTools.findAllThreads(".. ");

    }

    /**
     * Remove an AppSocket.
     */
    synchronized void removeAppSocket(AppSocket s) {
        int port = s.getLocalPort();

        // Logger.getLogger("log").logln(USR.ERROR, leadin() + "removeAppSocket " + port + "  -> " + s);

        // unregister the socket
        socketMap.remove(port);

        // remove the queue
        socketQueue.remove(port);


        // remove stats
        socketStats.remove(port);

        // TODO: free up port number for reuse
    }

    /**
     * Is a specified port number available
     */
    synchronized boolean isPortAvailable(int port) {
        // visit each socket and get its port
        for (AppSocket s : socketMap.values()) {
            if (port == s.getLocalPort()) {
                // the port is in use
                return false;
            }
        }

        // no one is using this port no
        return true;
    }

    /**
     * Find the next free port number.
     */
    synchronized int findNextFreePort() {
        // check if the next one is actually not free
        while (!isPortAvailable(freePort)) {
            freePort++;
        }

        // return freePort and skip to next one
        return freePort++;
    }

    /**
     * Get the queue for a specified port.
     */
    LinkedBlockingQueue<Datagram> getQueueForPort(int port) {
        return socketQueue.get(port);
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
        final String AS = "ASM: ";

        return controller.getName() + " " + AS;
    }

}
