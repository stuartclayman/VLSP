package usr.router;

import usr.console.*;
import java.util.Scanner;
import java.util.Queue;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.channels.SocketChannel;
import usr.protocol.*;
import usr.net.*;

/**
 * The Router Controller provides the management and control
 * mechanisms for the whole router.
 * <p>
 * Each RouterController starts on a different port from
 * any other RouterController, so that many can exist on
 * the same host.
 */
public class RouterController implements ComponentController, Runnable {
    
    //Has the address been set
    boolean addressSet_= false;
    
    // The Router this is a Controller for
    Router router;

    // The management console listener
    RouterManagementConsole management;

    // The port this router listening on for management
    int managementConsolePort;

    // The connections 
    RouterConnections connections;

    // The port the router listening on for new connections
    int newConnectionPort;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    // My name
    String name;

    // My Global ID
    int globalID= 0;

    // the no of connections
    int connectionCount;

    // Map of NetIFs that are in the process of being finalized
    // and are temporarily held here in the RouterController.
    HashMap<Integer, NetIF> tempNetIFMap;

    /**
     * Construct a RouterController, given a specific port.
     * The ManagementConsole listens on 'port' and
     * The Router to Router connections listens on port + 1.
     */
    public RouterController(Router router, int port) {
        this(router, port, port + 1);
    }

    /**
     * Construct a RouterController.
     * The ManagementConsole listens on 'mPort' and
     * The Router to Router connections listens on 'r2rPort'.
     */
    public RouterController(Router router, int mPort, int r2rPort) {
        this.router = router;

        name = "Router-" + mPort + "-" + r2rPort;
        globalID = name.hashCode();
        System.err.println("GID set initially "+globalID);
        

        this.managementConsolePort = mPort;

        // delegate listening of commands to a ManagementConsole object
        management = new RouterManagementConsole(this, mPort);

        newConnectionPort = r2rPort;

        // delegate listening for new connections to RouterConnections object
        connections = new RouterConnections(this, r2rPort);

        connectionCount = 0;

        // a map of NetIFs
        tempNetIFMap = new HashMap<Integer, NetIF>();

    }

    /**
     * Get the name of this RouterController.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this RouterController.
     * This can only be done before the Router has started to
     * communicate with other elements.
     * @return false if the name cannot be set
     */
    public boolean setName(String name) {
        if (connectionCount == 0) {
            this.name = name;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the global ID of this RouterController.
     */
    public int getGlobalID() {
        return globalID;
    }

    /**
     * Set the global ID of this RouterController.
     * This can only be done before the Router has started to
     * communicate with other elements.
     * @return false if the ID cannot be set
     */
    public boolean setGlobalID(int id) {
        if (connectionCount == 0) {
            this.globalID = id;
            return true;
        } else {
            return false;
        }
    }


    /** The address for the router can be set only if it has
    not been set before and the router has no connections*/
    public boolean canSetAddress() {
        if (addressSet_ == true)
             return false;
        if (connectionCount > 0)
            return false;
        return true;
    }

    /**
     * Get the no of connections that have been made.
     */
    public int getConnectionCount() {
        return connectionCount;
    }

    /**
     * Get the port for the ManagementConsole.
     */
    public int getManagementConsolePort() {
        return managementConsolePort;
    }

    /**
     * Get the port for the connection port
     */
    public int getConnectionPort() {
        return newConnectionPort;
    }

    /**
     * Get the ManagementConsole.
     */
    public ManagementConsole getManagementConsole() {
        return management;
    }

    /**
     * Start me up.
     */
    public boolean start() {
        System.out.println(leadin() + "start");

        // start my own thread
        myThread = new Thread(this);
        running = true;
        myThread.start();

        // start router to router connections listener
        boolean startedC = connections.start();

        // start management console listener
        boolean startedL = management.start();

        return startedL && startedC;
    }
    
    /**
     * Stop the RouterController.
     */
    public boolean stop() {
        System.out.println(leadin() + "stop");

        // stop the management console listener
        boolean stoppedL = management.stop();

        // stop the router to router connections
        boolean stoppedC = connections.stop();

        // stop my own thread
        running = false;
        myThread.interrupt();

        // wait for myself
        try {
            myThread.join();
        } catch (InterruptedException ie) {
            // System.err.println("RouterController: stop - InterruptedException for myThread join on " + myThread);
        }


        return stoppedL && stoppedC;
    }

    /**
     * Shutdown the Router.
     */
    public void shutDown() {
        // We have to stop the ManagementConsole and the RouterConnections
        // The we have to wait for this thread to terminate
        System.out.println(leadin() + "Shutdown");

        // stop other ManagementConsole and RouterConnections
        router.stop();

    }


    /**
     * The main thread loop.
     * It processes asynchronous commands from the request queue.
     * <p>
     * Asynchronous commands include:
     * CREATE_CONNECTION ip_addr/port - create a new network interface to
     * a router on the address ip_addr/port
     * 
     */
    public void run() {
        // get a handle on the Listener Queue
        BlockingQueue<Request> queue = management.queue();

        // create an Executor pool of size 3
        ExecutorService pool = Executors.newFixedThreadPool(3);

        while (running) {
            try {
                // we check the RouterListener queue for commands
                // TODO: maybe replace take() with poll(30, SECONDS)
                Request nextRequest = queue.take();
                String value = nextRequest.value;

                System.out.println(leadin() + "nextRequest = " + nextRequest);
                
                // now process the next request
                if (value.startsWith("CREATE_CONNECTION")) {
                    // incr connection count
                    connectionCount++;

                    pool.execute(new CreateConnection(this, nextRequest));

                } else if (value.startsWith("END_LINK")) {
                    // incr connection count
                    connectionCount--;
                    pool.execute(new EndLink(this,nextRequest));
                   
                    
                }  
                
                else {
                    System.err.println(leadin() + "Unknown request " + nextRequest);
                }
                    

            } catch (InterruptedException ie) {
                //System.err.println(leadin() + "BlockingQueue: interrupt " + ie);
            }
        }

        // shutdown Thread pool
        pool.shutdown();

    }

   
    /** Return a routing table as a string */
    public RoutingTable getRoutingTable() {
        return router.getRoutingTable();
    }

    /**
     * Keep a handle on a NetIF created from INCOMING_CONNECTION
     */
    synchronized void registerTemporaryNetIF(NetIF netIF) {
        int id = netIF.getID();

        System.out.println(leadin() + "addNetIF " + id + " for " + netIF);
        //System.err.println(leadin() + "addNetIF " + id + " for " + netIF);
        tempNetIFMap.put(id, netIF);

    }

    /**
     * Find a NetIF by an id.
     */
    public synchronized NetIF getTemporaryNetIFByID(int id) {
        //System.err.println(leadin() + "getNetIF " + id);
        return tempNetIFMap.get(id);
    }

    /**
     * List all the temporary connections held by the Controller
     */
    synchronized Collection<NetIF> listTempNetIF() {
        return tempNetIFMap.values();
    }

    /**
     * Plug a NetIF into the RouterFabric
     */
    public synchronized RouterPort plugTemporaryNetIFIntoPort(NetIF netIF) {
        RouterPort rp = router.plugInNetIF(netIF);
        //System.err.println(leadin() + "plugInNetIF "  + netIF);

        tempNetIFMap.remove(netIF.getID());

        return rp;
    }

    /** ping neighbours of router */
    
    public void pingNeighbours() 
    {
        byte []b= new byte[1];
        System.err.println("Pinging");
        Datagram dg= DatagramFactory.newDatagram(Protocol.DATA, ByteBuffer.wrap(b));
        List<NetIF> nif= router.listNetIF();
        if (nif == null)
            return;
        System.err.println("COLLECTION IS  "+nif);
        for (NetIF n : nif) {
            
            if (n.sendDatagram(dg) == true) {
                System.err.println("Ping sent");
            }
        }
    }
    
    /** Try to ping router with a given id */
    public boolean ping(int id){
        return router.ping(id);
    }


    /** Try to echo to a router with a given id */
    public boolean echo(int id){
        return router.echo(id);
    }
    
    /**
     * Get port N.
     */
    public RouterPort getPort(int p) {
        return router.getPort(p);
    }

    public void removeNetIF(NetIF n) {
       router.removeNetIF(n);
    }

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    public List<RouterPort> listPorts() {
        return router.listPorts();
    }

    /** Return the netIF associated with a certain router name
    */
    public NetIF findNetIF(String rName) {
        NetIF net= router.findNetIF(rName);
        if (net == null) {
            System.err.println(leadin()+" cannot find connection to "+ rName); 
        }
        return net;
    }
    
    /** Read a string containing router options */
    public boolean readOptionsString(String str) 
    {
        return router.readOptionsString(str);
    }
    
    /** Read a file containing router options */
    
    public boolean readOptionsFile(String fName)
    {
        return router.readOptionsFile(fName);
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RC = "RC: ";

        return getName() + " " + RC;
    }

    


}
