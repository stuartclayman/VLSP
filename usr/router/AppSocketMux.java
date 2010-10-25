package usr.router;

import usr.net.*;
import usr.logging.*;
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
    boolean theEnd= false;
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

    // stats for interface
    NetStats netStats;
    // stats for each socket
    HashMap<Integer, NetStats> socketStats;

    /**
     * Construct an AppSocketMux.
     */
    AppSocketMux(RouterController controller) {
        this.controller = controller;
        incomingQueue = new LinkedBlockingQueue<Datagram>();
        outgoingQueue = new LinkedBlockingQueue<Datagram>();
        socketMap = new HashMap<Integer, AppSocket>();
        socketQueue = new HashMap<Integer, LinkedBlockingQueue<Datagram>>();
        netStats = new NetStats();
        socketStats = new HashMap<Integer, NetStats>();
    }

    /**
     * Start me up.
     */
    public boolean start() {
        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

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
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");

        HashSet<AppSocket> sockets = new HashSet<AppSocket>(socketMap.values());

        for (AppSocket s : sockets) {
            s.close();
        }

        close();

        // stop my own thread
        running = false;
        myThread.interrupt();
        // Logger.getLogger("log").logln(USR.STDOUT, leadin() + "reached WaitFor");
        waitFor();

        // Logger.getLogger("log").logln(USR.STDOUT, leadin() + "reached end of stop");
        return true;
    }

    public void run() {
        while (running) {
            
            Datagram datagram;

            try {
                datagram = incomingQueue.take();
            } catch (InterruptedException ie) {
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "run INTERRUPTED");
                continue;
            }

            if (datagram.getProtocol() == Protocol.CONTROL) {
                datagramCount++;

                byte[] payload = datagram.getPayload();
                byte controlChar= payload[0];

                if (controlChar == 'C') {
                    //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Got Close");
                    remoteClose();
                }
            }
        
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + datagramCount + " GOT DATAGRAM from "  + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

            datagramCount++;

            // check the port of the socket and send it on
            int dstPort = datagram.getDstPort();

            // find the socket to deliver to
            AppSocket socket = socketMap.get(dstPort);

            if (socket != null) {
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "About to queue for " + socket);

                LinkedBlockingQueue<Datagram> queue = getQueueForPort(dstPort);
                queue.add(datagram);
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Queue for " + socket + " is size: " + queue.size());

                // now do stats
                NetStats stats = socketStats.get(dstPort);
                stats.increment(NetStats.Stat.InPackets);
                stats.add(NetStats.Stat.InBytes, datagram.getTotalLength());


            } else {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cant deliver to port " + dstPort);
                // so count dropped
                netStats.increment(NetStats.Stat.InDropped);
            }
        }
     
        theEnd();
    }

    /**
     * Wait for this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void waitFor() {
          try {
              //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"waiting");
            synchronized(this) {
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
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+"In a loop");
                Thread.sleep(100);
            } catch (Exception e) {
            
            }
        }
        
        
        
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"notifying");
        synchronized(this) {
            notify();
        }
        
    }

    synchronized void setTheEnd() {
        theEnd= true;
    }

    synchronized boolean ended() {
      return theEnd;  
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
     * Returns a NetStats object.
     */
    public NetStats getStats() {
        return netStats;
    }

    /**
     * Get the socket stats.
     * Returns a NetStats object for each socket, by port number
     */
    public Map<Integer, NetStats> getSocketStats() {
        // now add queues for sockets
        for (int port : socketStats.keySet()) {
            NetStats stats = socketStats.get(port);
            stats.setValue(NetStats.Stat.OutQueue, socketQueue.get(port).size());
        }

        return socketStats;
    }

    /**
     * Send a Datagram coming from the RouterFabric
     * and passes it to an AppSocket.
     */
    public  boolean sendDatagram(Datagram dg) {
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "datagramArrived: ");

        // stats
        netStats.increment(NetStats.Stat.InPackets);
        netStats.add(NetStats.Stat.InBytes, dg.getTotalLength());

        incomingQueue.add(dg);

        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Incoming queue size: " + incomingQueue.size());

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
        /* WAS
        try {
            Datagram datagram = outgoingQueue.take();

            // stats

            return datagram;
        } catch (InterruptedException ie) {
            // InterruptedException, return null
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "readDatagram INTERRUPTED");
            return null;
        }
        */
        return null;
    }


    /**
     * Sends a Datagram from an AppSocket towards to RouterFabric.
     */
    public boolean socketSendDatagram(Datagram datagram) {
        // patch up the source address in the Datagram
        Address srcAddr = controller.getAddress();
        datagram.setSrcAddress(srcAddr);

        // stats
        netStats.increment(NetStats.Stat.OutPackets);
        netStats.add(NetStats.Stat.OutBytes, datagram.getTotalLength());

        // do per socket stats
        int srcPort = datagram.getSrcPort();
        NetStats stats = socketStats.get(srcPort);

        if (stats != null) {
            stats.increment(NetStats.Stat.OutPackets);
            stats.add(NetStats.Stat.OutBytes, datagram.getTotalLength());
        }


        //outgoingQueue.add(datagram);

        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Outgoing queue size: " + outgoingQueue.size());

        // tell fabric we have a Datagram
        listener.datagramArrived(this,datagram);

        return true;
    }



    /**
     * Close a NetIF
     */
    public void close() {
        if (!isClosed()) {
            isClosed = true;
        }
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

        // Logger.getLogger("log").logln(USR.ERROR, leadin() + "addAppSocket " + port + "  -> " + s);


        // register the socket
        socketMap.put(port, s);

        // set up the incoming queue
        socketQueue.put(port, new LinkedBlockingQueue<Datagram>());

        // set up the stats
        socketStats.put(port, new NetStats());
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


