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
import java.io.IOException;
import java.util.concurrent.*;

/**
 * The AppSocketMux class allocates pseudo sockets as as application
 * layer function in order that applications can send data to each other
 * and have an address and a port.
 */
public class AppSocketMux implements NetIF {
    // the RouterController
    RouterController controller;
    boolean theEnd= false;
    // the count of the no of Datagrams
    int datagramCount = 0;
    // the next free port
    int freePort = 32768;

    // The list of all AppSockets
    HashMap<Integer, AppSocket>socketMap;

    // The queues of Datagrams for all AppSockets
    HashMap<Integer, LinkedBlockingQueue<Datagram>>socketQueue;

    // My Thread
    Thread myThread;
    boolean running = false;
    
    boolean removeRequested_= false;
    
    FabricDevice fabricDevice_= null;
   
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
        socketMap = new HashMap<Integer, AppSocket>();
        socketQueue = new HashMap<Integer, LinkedBlockingQueue<Datagram>>();
        socketStats = new HashMap<Integer, NetStats>();
    }

    /**
     * Start me up.
     */
    public boolean start() {
        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

            // start my own thread
            running = true;
            fabricDevice_= new FabricDevice(this, controller.getListener()); 
                //Inbound is 100 packets, blocking
                //Outbound is no queue                                  
            fabricDevice_.setInQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
            fabricDevice_.setInQueueLength(1000);
            fabricDevice_.setName("ASM");
            fabricDevice_.start();
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
    public synchronized boolean stop() {
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
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"In a loop");
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
        setWeight(0);

        // now plug it in to the Router Fabric
        controller.registerTemporaryNetIF(this);
        controller.plugTemporaryNetIFIntoPort(this);

        return true;
    }
    
    /** The fabric device which moves packets to/from this interface */
    public FabricDevice getFabricDevice()
    {
        return fabricDevice_;
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
    public void setName(String name) {
        name = name;
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
    public  void setID(int id) {
        this.id = id;
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
    public void setWeight(int w) {
        weight = w;
    }

    /**
     * Get the Address for this connection.
     */
    public Address getAddress() {
        return controller.getAddress();
    }

    /**
     * Set the Address for this connection.
     */
    public void setAddress(Address addr) {
        System.err.println("DO NOT SET ADDRESS FOR ASM");
        System.exit(-1);
    }

    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    public String getRemoteRouterName() {
        return controller.getName();
    }


    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterName(String name) {
        System.err.println("Do not call setRemoteRouterName for ASM");
        System.exit(-1);
    }

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    public Address getRemoteRouterAddress() {
        return controller.getAddress();
    }

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterAddress(Address addr) {
        System.err.println("Do not call setRemoteRouterAddress for ASM");
        System.exit(-1);
    }

    /**
     * Get the interface stats.
     * Returns a NetStats object.
     */
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
    public void close() {
        if (!isClosed()) {
            isClosed = true;
        }
    }

    public boolean isLocal() {
        return true;
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
    public void setNetIFListener(NetIFListener l) {
        listener = l;
        fabricDevice_.setListener(l);

    }
    
    /** Remote close received */
    public void remoteClose() {
        close();
    }
   
    /**
     * Send a Datagram -- sets source to this interface and puts the datagram
     on the incoming queue for this interface
     */
    public boolean sendDatagram(Datagram dg) throws NoRouteToHostException{
        if (running == true) {
            // set the source address and port on the Datagram
            dg.setSrcAddress(getAddress());
            return enqueueDatagram(dg);
        } else {
            return true;
        }
    }
    
    /**
     * Puts a datagram on the incoming queue for this network interface
     */
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException {
        return fabricDevice_.blockingAddToInQueue(dg,this);
    }


    /** 
        Deliver a received datagram to the appropriate app
    */
    public synchronized boolean outQueueHandler(Datagram datagram, DatagramDevice device) 
    {
       // if (running == false)  // If we're not running simply pretend to have received it
       //     return true;
        if (datagram.getProtocol() == Protocol.CONTROL) {
            datagramCount++;
            byte[] payload = datagram.getPayload();
            byte controlChar= payload[0];
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
                "Can't deliver to port " + dstPort);
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
        s.localAddress = getAddress();

        int port = s.getLocalPort();

      //  Logger.getLogger("log").logln(USR.ERROR, leadin() + "addAppSocket " + port + "  -> " + s);


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
