package usr.router;

import usr.net.*;
import usr.protocol.Protocol;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.IOException;

/**
 * The AppSocketMux class allocates pseudo sockets as as application
 * layer function in order that applications can send data to each other
 * and have an address and a port.
 */
public class AppSocketMux implements NetIF, Runnable {
    // the RouterController
    RouterController controller;

    // the count of the no of Datagrams
    int datagramCount = 0;

    // the next free port
    int freePort = 32768;

    // Incoming queue
    LinkedBlockingQueue<Datagram> incomingQueue;

    // Outgoing queue
    LinkedBlockingQueue<Datagram> outgoingQueue;

    // The list of all AppSockets
    HashMap<Integer, AppSocket>socketMap;

    // The queues of Datagrams for all AppSockets
    HashMap<Integer, LinkedBlockingQueue<Datagram>>socketQueue;

    // My Thread
    Thread myThread;
    boolean running = false;
    
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
    AppSocketMux(RouterController controller) {
        this.controller = controller;
        incomingQueue = new LinkedBlockingQueue<Datagram>();
        outgoingQueue = new LinkedBlockingQueue<Datagram>();
        socketMap = new HashMap<Integer, AppSocket>();
        socketQueue = new HashMap<Integer, LinkedBlockingQueue<Datagram>>();

    }

    /**
     * Start me up.
     */
    public boolean start() {
        try {
            System.out.println(leadin() + "start");

            // start my own thread
            myThread = new Thread(this);
            running = true;
            myThread.start();

            boolean connected = connect();

            return connected;
        } catch (Exception e) {
            running = false;
            return false;
        }
    }
    
    /**
     * Close all sockets.
     */
    public boolean stop() {
        System.out.println(leadin() + "stop");

        HashSet<AppSocket> sockets = new HashSet<AppSocket>(socketMap.values());

        for (AppSocket s : sockets) {
            s.close();
        }

        close();

        // stop my own thread
        running = false;
        myThread.interrupt();

        waitFor();

        /*
        // wait for myself
        try {
            myThread.join();
        } catch (InterruptedException ie) {
            // System.err.println(leadin() + "stop - InterruptedException for myThread join on " + myThread);
        }
        */

        return true;
    }

    public void run() {
        while (running) {
            
            Datagram datagram;

            try {
                datagram = incomingQueue.take();
            } catch (InterruptedException ie) {
                //System.err.println(leadin() + "run INTERRUPTED");
                continue;
            }

            if (datagram.getProtocol() == Protocol.CONTROL) {
                datagramCount++;

                byte[] payload = datagram.getPayload();
                byte controlChar= payload[0];

                if (controlChar == 'C') {
                    //System.err.println(leadin() + "Got Close");
                    remoteClose();
                }
            }
        
            //System.err.println(leadin() + datagramCount + " GOT DATAGRAM from "  + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

            datagramCount++;

            // check the port of the socket and send it on
            int dstPort = datagram.getDstPort();

            // find the socket to deliver to
            AppSocket socket = socketMap.get(dstPort);

            if (socket != null) {
                //System.err.println(leadin() + "About to queue for " + socket);

                LinkedBlockingQueue<Datagram> queue = getQueueForPort(dstPort);
                queue.add(datagram);
                //System.err.println(leadin() + "Queue for " + socket + " is size: " + queue.size());
            } else {
                System.err.println(leadin() + "Cant deliver to port " + dstPort);
            }
        }

        theEnd();
    }

    /**
     * Wait for this thread.
     */
    private synchronized void waitFor() {
        // System.out.println(leadin() + "waitFor");
        try {
            wait();
        } catch (InterruptedException ie) {
        }
    }
    
    /**
     * Notify this thread.
     */
    private synchronized void theEnd() {
        // System.out.println(leadin() + "theEnd");
        notify();
    }


    private synchronized void pause() {
        try {
            wait();
        } catch (InterruptedException ie) {
        }
    }

    /**
     * Connect to my local Router.
     */
    public boolean connect() throws IOException {
        setID(0);
        setName("localnet");
        setAddress(new GIDAddress(0));
        setRemoteRouterName(controller.getName());
        setRemoteRouterAddress(new GIDAddress(0));
        setWeight(0);

        // now plug it in to the Router Fabric
        controller.registerTemporaryNetIF(this);
        controller.plugTemporaryNetIFIntoPort(this);

        return true;
    }

    /**
     * Get the name of this NetIF.
     */
    public String getName() {
        return name;
    }


    /**
     * Set the name of this NetIF.
     */
    public NetIF setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the ID of this NetIF.
     */
    public int getID() {
        return id;
    }

    /**
     * Set the ID of this NetIF.
     */
    public NetIF setID(int id) {
        this.id = id;
        return this;
    }

    /**
     * Get the weight of this NetIF.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Set the weight of this NetIF.
     */
    public NetIF setWeight(int w) {
        weight = w;
        return this;
    }

    /**
     * Get the Address for this connection.
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Set the Address for this connection.
     */
    public NetIF setAddress(Address addr) {
        address = addr;
        return this;
    }

    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    public String getRemoteRouterName() {
        return remoteName;
    }


    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    public NetIF setRemoteRouterName(String name) {
        remoteName = name;
        return this;
    }

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    public Address getRemoteRouterAddress() {
        return remoteAddress;
    }

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    public NetIF setRemoteRouterAddress(Address addr) {
        remoteAddress = addr;
        return this;
    }

    /**
     * Get the interface stats.
     * A map of values like:
     * "in_bytes" -> in_bytes
     * "in_packets" -> in_packets
     * "in_errors" -> in_errors
     * "in_dropped" -> in_dropped
     * "out_bytes" -> out_bytes
     * "out_packets" -> out_packets
     * "out_errors" -> out_errors
     * "out_dropped" -> out_dropped
     */
    public Map<String, Number> getStats() {
        return null;
    }

    /**
     * Send a Datagram coming from the RouterFabric
     * and passes it to an AppSocket.
     */
    public  boolean sendDatagram(Datagram dg) {
        //System.err.println(leadin() + "datagramArrived: ");

        incomingQueue.add(dg);

        //System.err.println(leadin() + "Incoming queue size: " + incomingQueue.size());

        return true;
    }

    /**
     * forward a datagram (does not set src address)
     */
    public boolean forwardDatagram(Datagram dg) {
        return sendDatagram(dg);
    }



    /**
     * Reads a Datagram sent from an AppSocket
     * into the RouterFabric.
     */
    public Datagram readDatagram() {
        try {
            Datagram datagram = outgoingQueue.take();

            return datagram;
        } catch (InterruptedException ie) {
            // InterruptedException, return null
            System.err.println(leadin() + "readDatagram INTERRUPTED");
            return null;
        }
    }


    /**
     * Sends a Datagram from an AppSocket towards to RouterFabric.
     */
    public boolean socketSendDatagram(Datagram datagram) {
        // patch up the source address in the Datagram
        GIDAddress srcAddr = controller.getAddress();
        datagram.setSrcAddress(srcAddr);

        outgoingQueue.add(datagram);

        //System.err.println(leadin() + "Outgoing queue size: " + outgoingQueue.size());

        // tell fabric we have a Datagram
        listener.datagramArrived(this);

        return true;
    }



    /**
     * Close a NetIF
     */
    public void close() {
        isClosed = true;
    }

    /**
     * Is closed.
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Get the Listener of a NetIF.
     */
    public NetIFListener getNetIFListener() {
        return listener;
    }

    /**
     * Set the Listener of NetIF.
     */
    public NetIF setNetIFListener(NetIFListener l) {
        listener = l;
        return this;
    }
    
    /** Setter function for remoteclose*/
    public void setRemoteClose(boolean rc)  {
    }

    /** Remote close received */
    public void remoteClose() {
        close();
    }
    
    /** Routing table sent */
    public boolean sendRoutingTable(String s) {
        throw new UnsupportedOperationException("AppSocketMux has no sendRoutingTable capability");
    }
    
    


    /**
     * Add an AppSocket.
     */
    synchronized void addAppSocket(AppSocket s) {
        s.localAddress = getAddress();

        int port = s.getLocalPort();

        // System.err.println(leadin() + "addAppSocket " + port + "  -> " + s);


        // register the socket
        socketMap.put(port, s);

        // set up the incoming queue
        socketQueue.put(port, new LinkedBlockingQueue<Datagram>());
    }

    /**
     * Remove an AppSocket.
     */
    synchronized void removeAppSocket(AppSocket s) {
        int port = s.getLocalPort();

        // System.err.println(leadin() + "removeAppSocket " + port + "  -> " + s);

        // unregister the socket
        socketMap.remove(port);

        // remove the queue
        socketQueue.remove(port);

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
        while (! isPortAvailable(freePort)) {
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
     * Create the String to print out before a message
     */
    String leadin() {
        final String AS = "ASM: ";

        return controller.getName() + " " + AS;
    }


}
