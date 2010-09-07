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

/**
 * The Router Controller provides the management and control
 * mechanisms for the whole router.
 * <p>
 * Each RouterController starts on a different port from
 * any other RouterController, so that many can exist on
 * the same host.
 */
public class RouterController implements ComponentController, Runnable {
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

    // the no of connections
    int connectionCount;

    // Map of NetIFs
    HashMap<Integer, NetIF> netIFMap;

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

        name = "Router" + hashCode();

        this.managementConsolePort = mPort;

        // delegate listening of commands to a ManagementConsole object
        management = new RouterManagementConsole(this, mPort);

        newConnectionPort = r2rPort;

        // delegate listening for new connections to RouterConnections object
        connections = new RouterConnections(this, r2rPort);

        connectionCount = 0;

        // a map of NetIFs
        netIFMap = new HashMap<Integer, NetIF>();

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
        waitFor();

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

                } else {
                    System.err.println(leadin() + "Unknown request " + nextRequest);
                }
                    

            } catch (InterruptedException ie) {
                //System.err.println(leadin() + "BlockingQueue: interrupt " + ie);
            }
        }

        // shutdown Thread pool
        pool.shutdown();

        // notify we have reached the end of this thread
        theEnd();
    }


    /**
     * Keep a handle on a NetIF created from INCOMING_CONNECTION
     */
    public void addNetIF(NetIF netIF) {
        int id = netIF.getID();

        System.err.println(leadin() + "addNetIF " + id + " for " + netIF);

        netIFMap.put(id, netIF);

    }

    /** Return a routing table as a string */
    public String listRoutingTable() {
        return router.listRoutingTable();
    }

    /**
     * Find a NetIF by an id.
     */
    public NetIF getNetIFByID(int id) {
        //System.err.println(leadin() + "getNetIF " + id);
        return netIFMap.get(id);
    }

    /**
     * List all the temporary connections held by the Controller
     */
    public Collection<NetIF> listNetIF() {
        return netIFMap.values();
    }

    /**
     * Plug a NetIF into the RouterFabric
     */
    public RouterPort plugInNetIF(NetIF netIF) {
        RouterPort rp = router.plugInNetIF(netIF);
        System.err.println(leadin() + "removeNetIFLocal "  + netIF);

        netIFMap.remove(netIF.getID());

        return rp;
    }

    /**
     * Get port N.
     */
    public RouterPort getPort(int p) {
        return router.getPort(p);
    }

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    public List<RouterPort> listPorts() {
        return router.listPorts();
    }


    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RC = "RC: ";

        return getName() + " " + RC;
    }




}
