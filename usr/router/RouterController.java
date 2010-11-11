package usr.router;

import usr.console.*;
import usr.logging.*;
import usr.applications.ApplicationHandle;
import usr.applications.ApplicationResponse;
import usr.applications.ApplicationManager;
import usr.applications.Ping;
import java.util.Scanner;
import java.util.Queue;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.channels.SocketChannel;
import usr.protocol.*;
import usr.net.*;
import usr.APcontroller.*;

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

    // My Address - a special feature where the router has its own address
    Address myAddress;

    // the no of connections
    int connectionCount;

    // Information about the APcontroller
    APController apController_= null;
    APInfo apInfo_= null; 
    int ap_= 0; // The aggregation point for this node
    String apName_= null;
    String monGenName_= null;

    RouterOptions options_= null;

    // Map of NetIFs that are in the process of being finalized
    // and are temporarily held here in the RouterController.
    HashMap<Integer, NetIF> tempNetIFMap;

    /**
     * Construct a RouterController, given a specific port.
     * The ManagementConsole listens on 'port' and
     * The Router to Router connections listens on port + 1.
     */
    public RouterController(Router router,  RouterOptions o, int port) {
        this(router, o, port, port + 1);
    }

    /**
     * Construct a RouterController.
     * The ManagementConsole listens on 'mPort' and
     * The Router to Router connections listens on 'r2rPort'.
     */
    public RouterController(Router router, RouterOptions o,
      int mPort, int r2rPort) {
      
        options_= o;
        this.router = router;

        name = "Router-" + mPort + "-" + r2rPort;
        myAddress = new GIDAddress(name.hashCode());

        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Address set initially "+globalID);
        

        this.managementConsolePort = mPort;

        // delegate listening of commands to a ManagementConsole object
        management = new RouterManagementConsole(this, mPort);

        newConnectionPort = r2rPort;

        // delegate listening for new connections to RouterConnections object
        connections = new RouterConnections(this, r2rPort);

        connectionCount = 0;

        // a map of NetIFs
        tempNetIFMap = new HashMap<Integer, NetIF>();

        // Set up info for AP management
        //System.out.println("Construct AP Controller");
        apController_= ConstructAPController.constructAPController
            (options_);
        apInfo_= apController_.newAPInfo();

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
    int getGlobalID() {
        return myAddress.asInteger();
    }

    /**
     * Set the global ID of this RouterController.
     * This can only be done before the Router has started to
     * communicate with other elements.
     * @return false if the ID cannot be set
     */
    boolean setGlobalID(int id) {
        return setAddress(new GIDAddress(id));
    }

    /**
     * Get the router address.
     * This is a special featrue for where the router has its own address.
     */
    public Address getAddress() {
        return myAddress;
    }

    /**
     * Set the router address.
     * This is a special feature where the router itself has its own address.
     */
    public boolean setAddress(Address addr) {
        if (connectionCount == 0) {
            myAddress = addr;
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
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

        // start my own thread
        myThread = new Thread(this, this.getClass().getName() + "-" + hashCode());
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
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");

        // stop applications
        stopApplications();

        // stop the management console listener
        boolean stoppedL = management.stop();

        // stop the router to router connections
        boolean stoppedC = connections.stop();

        // stop my own thread
       // running = false;
       // myThread.interrupt();

        // wait for myself
        //try {
        //    myThread.join();
        //} catch (InterruptedException ie) {
            // Logger.getLogger("log").logln(USR.ERROR, "RouterController: stop - InterruptedException for myThread join on " + myThread);
        //}


        return stoppedL && stoppedC;
    }

    /**
     * Shutdown the Router.
     */
    public void shutDown() {
        // We have to stop the ManagementConsole and the RouterConnections
        // The we have to wait for this thread to terminate
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Shutdown");

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

        // create an Executor pool
        ExecutorService pool = Executors.newCachedThreadPool();  // WAS newFixedThreadPool(3);
        long now= System.currentTimeMillis();
        long next= now + options_.getRouterConsiderTime();
        boolean shutDown= false;
        while (running) {
            try {
                now= System.currentTimeMillis();
                if (now >= next) {
                    next+= options_.getRouterConsiderTime();
                    apController_.routerUpdate(this);
                }
                // we check the RouterListener queue for commands
                Request nextRequest = queue.poll(next-now,TimeUnit.MILLISECONDS);
                if (nextRequest == null)
                    continue;
                String value = nextRequest.value;

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + " Controller processing nextRequest = " + nextRequest);
                
                // now process the next request
                if (value.startsWith("CREATE_CONNECTION")) {
                    // incr connection count
                    connectionCount++;

                    pool.execute(new CreateConnection(this, nextRequest));

                } else if (value.startsWith("END_LINK")) {
                    // do not decrease connectionCount, it MUST only increase
                    pool.execute(new EndLink(this,nextRequest));
                   
                    
                }  else if (value.startsWith("SHUT_DOWN")) {
                    shutDown= true;
                    break;
                }
                
                else {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown request " + nextRequest);
                }
                    

            } catch (InterruptedException ie) {
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "BlockingQueue: interrupt " + ie);
            }
        }
        if (shutDown) {
            // shutdown Thread pool
            pool.shutdown();
            shutDown();
        }
        

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

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "temporary addNetIF " + id + " for " + netIF);
        tempNetIFMap.put(id, netIF);

    }

    /**
     * Find a NetIF by an id.
     */
    public synchronized NetIF getTemporaryNetIFByID(int id) {
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "getNetIF " + id);
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
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "plugInNetIF "  + netIF);

        tempNetIFMap.remove(netIF.getID());

        return rp;
    }

    /**
     * Start an App.
     * It takes a class name and some args.
     * It returns app_name ~= /Router-1/App/class.blah.Blah/1
     */    
    public ApplicationResponse appStart(String commandstr) {
        String[] split= commandstr.split(" ");

        if (split.length == 0)  {
            return new ApplicationResponse(false, "appStart needs application class name");
        } else {
            String className = split[0].trim();

            String rest = commandstr.substring(className.length()).trim();
            String[] args= rest.split(" ");

            return ApplicationManager.startApp(className, args);
        }
    }

    /**
     * Stop an App.
     * It takes an app name
     */    
    public ApplicationResponse appStop(String commandstr) {
        String [] split= commandstr.split(" ");
        if (split.length == 0) {
            return new ApplicationResponse(false, "appStop needs application name");
        } else {
            String appName = split[0].trim();

            return ApplicationManager.stopApp(appName);
        }
    }

    /**
     * List all App.
     * It takes an app name
     */    
    public Collection<ApplicationHandle> appList() {
        return ApplicationManager.listApps();
    }

    /** Stop running applications if any */
    void stopApplications () {
        ApplicationManager.stopAll();
    }


    /** run a particular command on a router*/
    public ApplicationResponse runCommand(String commandstr) {
        String [] split= commandstr.split(" ");
        if (split.length == 0) 
            return new ApplicationResponse(false, "No command specified");

        String command= split[0].trim();
        String rest = commandstr.substring(command.length()).trim();

        ApplicationResponse response;

        if (command.equals("PING")) {
            response = appStart("usr.applications.Ping " + rest);
        } else {
            return new ApplicationResponse(false, "Unknown command " + command);
        }

        return response;
        
    }

    /** ping neighbours of router */
    public void pingNeighbours() 
    {
        router.pingNeighbours();
        
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
     * Get the local NetIF that has the sockets.
     */
    public NetIF getLocalNetIF() {
        return router.getLocalNetIF();
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

    /** Return the netIF associated with a certain router name
    */
    public NetIF findNetIF(String rName) {
        NetIF net= router.findNetIF(rName);
        if (net == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" cannot find connection to "+ rName); 
        }
        return net;
    }
    
    /** Remove a NetIF */
    public void removeNetIF(NetIF n) {
       router.removeNetIF(n);
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

    /** Set the aggregation point for this router */
    public boolean setAP(int gid, int ap) 
    
    { 
        if (gid != getGlobalID())
            return false;
        if (ap == ap_)    // No change to AP
            return true;
        if (monGenName_ != null) { // stop previous monitoring generator
            appStop(monGenName_);
        }
        ApplicationResponse resp= appStart("plugins_usr.aggregator.appl.InfoSource -o "+ap+
            "/3000 -p rt -t 1 -d 3 -n info-source-"+gid);
        monGenName_= resp.getMessage();
        System.out.println(leadin()+" now has aggregation point "+ap);
        if (gid == ap && ap_ != ap) {
            startAP(ap);
        } else if (ap_ == gid && ap_ != ap) {
            stopAP();
        }
        ap_= ap;
        return true;
    }
     
    /** This node starts as an AP */
    public void startAP(int gid) {
        System.out.println(leadin()+" has become an AP");
        ApplicationResponse resp= appStart("plugins_usr.aggregator.appl.AggPoint -i 0/3000"+
        " -t 5 -a average -n agg-point-"+gid); 
        apName_= resp.getMessage();
    }
    
    /** This node stops as an AP*/
    public void stopAP() {
        System.out.println(leadin()+" has stopped being an AP");
        appStop(apName_);
    }
    

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RC = "RC: ";

        return getName() + " " + RC;
    }

    


}
