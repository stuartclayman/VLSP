package usr.router;

import usr.console.*;
import cc.clayman.console.ManagementConsole;
import usr.logging.*;
import usr.applications.ApplicationHandle;
import usr.applications.ApplicationResponse;
import usr.applications.ApplicationManager;
import usr.applications.Ping;
import java.util.Scanner;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;
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
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneProducerWithNames;
import eu.reservoir.monitoring.appl.BasicDataSource;
import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import eu.reservoir.monitoring.appl.datarate.EveryNMilliseconds;
import java.net.InetSocketAddress;
import java.lang.reflect.Constructor;


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
    boolean addressSet_ = false;

    // The Router this is a Controller for
    Router router;

    // The management console listener
    RouterManagementConsole management;

    // The port this router listening on for management
    int managementConsolePort;

    // The connections
    RouterConnections connections;

    // The port the router listening on for new connections
    //int newConnectionPort;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    // My name
    String name = null;

    // My Address - a special feature where the router has its own address
    Address myAddress;

    // the no of connections
    int connectionCount;

    // ApplicationSocket Multiplexor
    AppSocketMux appSocketMux;

    // The ApplicationManager
    ApplicationManager appManager;


    // The ThreadGroup
    ThreadGroup threadGroup;

    // Counts ensure that Info Source and Aggreg Points have unique names
    int isCount_ = 1;
    int apCount_ = 1;

    // Information about the APcontroller
    APController apController_ = null;
    APInfo apInfo_ = null;
    int ap_ = 0; // The aggregation point for this node
    String apName_ = null;
    String monGenName_ = null;

    RouterOptions options_ = null;

    // Map of NetIFs that are in the process of being finalized
    // and are temporarily held here in the RouterController.
    HashMap<Integer, NetIF> tempNetIFMap;

    // A BasicDataSource for the stats of a Router
    BasicDataSource dataSource = null;

    // The probes
    ArrayList<RouterProbe> probeList = null;

    /**
     * Construct a RouterController, given a specific port.
     * The ManagementConsole listens on 'port' and
     * The Router to Router connections listens on port + 1.
     */
    public RouterController(Router router, RouterOptions o, int port, String name) {
        this(router, o, port, port + 1, name);
    }

    /**
     * Construct a RouterController.
     * The ManagementConsole listens on 'mPort' and
     * The Router to Router connections listens on 'r2rPort'.
     */
    public RouterController(Router router, RouterOptions o, int mPort, int r2rPort, String name) {

        options_ = o;
        this.router = router;

        this.name = name;

        try {
            myAddress = AddressFactory.newAddress(name.hashCode());
        } catch (java.net.UnknownHostException e) {
            myAddress = null;
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot make address from "+ name.hashCode());
        }

        this.managementConsolePort = mPort;
        // delegate listening of commands to a ManagementConsole object
        management = new RouterManagementConsole(this, mPort);

        //newConnectionPort = r2rPort;

        // delegate listening for new connections to RouterConnections object
        connections = new RouterConnections(this, r2rPort);
        connectionCount = 0;
        // a map of NetIFs
        tempNetIFMap = new HashMap<Integer, NetIF>();

        // setup ApplicationManager
        appManager = new ApplicationManager(router);

        // ThreadGroup
        threadGroup = router.getThreadGroup();

        // Set up info for AP management
        //System.out.println("Construct AP Controller");
        apController_ = ConstructAPController.constructAPController(options_);
        apInfo_ = apController_.newAPInfo();

        // setup DataSource
        dataSource = new BasicDataSource(name + ".dataSource");
        probeList = new ArrayList<RouterProbe>();

        System.out.println(leadin() + "Setup DataSource: " + dataSource);
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
        if (addressSet_ == true) {
            return false;
        }

        if (connectionCount > 0) {
            return false;
        }
        return true;
    }

    /**
     * Get the no of connections that have been made.
     */
    public int getConnectionCount() {
        return connectionCount;
    }

    /**
     * Increment the no of connections that have been made.
     */
    public int newConnection() {
        return ++connectionCount;
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
        return connections.getConnectionPort();
    }

    /**
     * Get the ManagementConsole.
     */
    public ManagementConsole getManagementConsole() {
        return management;
    }

    /**
     * Get the Thread Group.
     */
    ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    /**
     * Start me up.
     */
    public boolean start() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

        // start my own thread
        myThread = new Thread(threadGroup, this, "/" + router.getName() + "/RouterController/" + hashCode());
        running = true;
        myThread.start();

        // start router to router connections listener
        boolean startedC = connections.start();

        // start management console listener
        boolean startedL = management.start();

        // start appSocketMux
        appSocketMux = new AppSocketMux(this);
        appSocketMux.start();

        return startedL && startedC;
    }

    /**
     * Stop the RouterController.
     */
    public boolean stop() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");

        if (options_.gracefulExit()) {
            router.sendGoodbye();
        }

        // stop applications
        stopApplications();

        // stop the dataSource and associated probe
        if (dataSource.isConnected()) {
            for (RouterProbe probe : probeList) {
                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + " data source: " +  dataSource.getName() + " notify stopped " +
                                              probe.getName());

                probe.stopped();
            }

            stopMonitoring();
        }


        // stop the appSocketMux
        appSocketMux.stop();

        // stop the management console listener
        //System.err.println("Management stop");
        boolean stoppedL = management.stop();
        //System.err.println("Connection stop");
        // stop the router to router connections
        boolean stoppedC = connections.stop();

        // stop my own thread
        running = false;

        myThread.interrupt();

        // wait for myself
        try {
            myThread.join();
        } catch (InterruptedException ie) {
            // Logger.getLogger("log").logln(USR.ERROR, "RouterController: stop - InterruptedException for myThread join on " +
            // myThread);
        }

        return stoppedL && stoppedC;
    }

    /** Shut down the router from internal message from console -- pass the message up to the router object */
    public void shutDown() {
        router.shutDown();
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
        /** Not needed with rest

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

                }
                else {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown request " + nextRequest);
                }


            } catch (InterruptedException ie) {
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "BlockingQueue: interrupt " + ie);
            }
           }
           // System.err.println("Pool stop");
           pool.shutdown();
         */
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

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "temporary addNetIF ID: " + id + "address: " + netIF.getAddress() + " for " + netIF);
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
    public RouterPort plugTemporaryNetIFIntoPort(NetIF netIF) {
        RouterPort rp = router.plugInNetIF(netIF);
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "plugInNetIF "  + netIF);

        tempNetIFMap.remove(netIF.getID());

        return rp;
    }

    /**
     * Get the AppSockMux this talks to.
     */
    AppSocketMux getAppSocketMux() {
        return appSocketMux;
    }


    /**
     * Start an App.
     * It takes a class name and some args.
     * It returns app_name ~= /Router-1/App/class.blah.Blah/1
     */
    public synchronized ApplicationResponse appStart(String commandstr) {
        String[] split = commandstr.split(" ");

        if (split.length == 0) {
            return new ApplicationResponse(false, "appStart needs application class name");
        } else {
            String className = split[0].trim();

            String rest = commandstr.substring(className.length()).trim();
            String[] args = rest.split(" ");

            return appManager.startApp(className, args);
        }
    }

    /**
     * Stop an App.
     * It takes an app name
     */
    public synchronized ApplicationResponse appStop(String commandstr) {
        String [] split = commandstr.split(" ");

        if (split.length == 0) {
            return new ApplicationResponse(false, "appStop needs application name");
        } else {
            String appName = split[0].trim();

            return appManager.stopApp(appName);
        }
    }

    /**
     * List all App.
     * It takes an app name
     */
    public Collection<ApplicationHandle> appList() {
        return appManager.listApps();
    }

    /**
     * Get an ApplicationHandle for an Application.
     */
    public ApplicationHandle findAppInfo(String name) {
        return appManager.find(name);
    }

    /** Stop running applications if any */
    void stopApplications () {
        appManager.stopAll();
    }

    /** Access the listener */
    public NetIFListener getListener() {
        return router.getListener();
    }

    /** run a particular command on a router*/
    public ApplicationResponse runCommand(String commandstr) {
        String [] split = commandstr.split(" ");

        if (split.length == 0) {
            return new ApplicationResponse(false, "No command specified");
        }

        String command = split[0].trim();
        String rest = commandstr.substring(command.length()).trim();

        ApplicationResponse response;

        if (command.equals("PING")) {
            response = appStart("usr.applications.Ping " + rest);
        } else {
            return new ApplicationResponse(false, "Unknown command " + command);
        }

        return response;

    }

    /** Try to ping router with a given Address */
    public boolean ping(Address addr) {
        return router.ping(addr);
    }

    /** Try to echo to a router with a given Address */
    public boolean echo(Address addr) {
        return router.echo(addr);
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
        NetIF net = router.findNetIF(rName);

        if (net == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" cannot find connection to "+ rName);
        }
        return net;
    }

    /** Set the netIF weight associated with a link to a certain router name
     */
    public boolean setNetIFWeight(String rName, int weight) {
        boolean set = router.setNetIFWeight(rName, weight);

        if (set == false) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+" cannot find connection to "+ rName);
        }
        return set;
    }

    /** Remove a NetIF */
    public void removeNetIF(NetIF n) {
        router.removeNetIF(n);
    }

    /** Read a string containing router options */
    public boolean readOptionsString(String str) {
        boolean read = router.readOptionsString(str);

        //System.out.println(leadin() + "options_.latticeMonitoring: " + options_.latticeMonitoring);

        // Determine monitoring setup
        if (options_.latticeMonitoring()) {
            // and probes
            setupProbes(options_.getProbeInfoMap());
        }

        return read;
    }

    /** Read a file containing router options */

    public boolean readOptionsFile(String fName) {
        return router.readOptionsFile(fName);
    }

    /** Set the aggregation point for this router */
    public synchronized void setAP(int gid, int ap) {
        if (ap == ap_) {  // No change to AP
            return;
        }

        if (monGenName_ != null) { // stop previous monitoring generator
            //System.err.println("APP STOP");
            appStop(monGenName_);

        }

        if (gid == ap) {  // If this is becoming an AP then start an AP
            startAP(ap);
        } else if (ap_ == gid) { // If this WAS an AP and is no longer then stop an AP
            stopAP();
        }
        ap_ = ap;

        // Now start an info source pointing at the new AP.
        String command = new String("plugins_usr.aggregator.appl.InfoSource -o "+ap+
                                    "/3000 -t 1 -d 3");
        command += (" -p "+options_.getMonType());    // What type of data do we monitor
        //command+= (" -n info-source-"+gid+"-"+isCount_);  // Make source name unique
        command += (" -n info-source-"+gid);  // Make source name

        if (options_.getAPFilter() != null) {
            command += (" -f "+options_.getAPFilter());             // Filter output
        }

        if (options_.getAPOutputPath() != null) {
            command += " -l "+ options_.getAPOutputPath();
        }
        ApplicationResponse resp = appStart(command);
        // WAS "/3000 -p rt -t 1 -d 3 -n info-source-"+gid+"-"+isCount_);
        isCount_++;
        monGenName_ = resp.getMessage();
        //System.err.println("NEW NAME "+monGenName_);
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+" now has aggregation point "+ap);

    }

    /** This node starts as an AP */
    public void startAP(int gid) {
        synchronized (this) {
            System.out.println(leadin()+" has become an AP");
            String command = new String("plugins_usr.aggregator.appl.AggPoint -i 0/3000 -t 5 -a average");
            command += (" -n agg-point-"+gid+"-"+apCount_);

            if (options_.getAPOutputPath() != null) {
                command += " -l "+ options_.getAPOutputPath();
            }
            ApplicationResponse resp = appStart(command);
            // WAS " -t 5 -a average -n agg-point-"+gid+"-"+apCount_);
            apCount_++;
            apName_ = resp.getMessage();
        }
    }

    /** This node stops as an AP*/
    public void stopAP() {
        synchronized (this) {
            System.out.println(leadin()+" has stopped being an AP");
            appStop(apName_);
            apName_ = null;
        }
    }

    /*
     * Stuff to do with monitoring
     */

    /**
     * Start monitoring.
     * Sends to a particular UDP address, and
     * sets the initial gap between transmits at every 'when' seconds.
     */
    public synchronized void startMonitoring(InetSocketAddress addr, int when) {
        // check to see if the monitoring is already connected and running
        if (dataSource.isConnected()) {
            // if it is, stop it first
            stopMonitoring();
        }

        // set up DataPlane
        DataPlane outputDataPlane = new UDPDataPlaneProducerWithNames(addr);
        dataSource.setDataPlane(outputDataPlane);

        // add probes
        for (RouterProbe probe : probeList) {
            dataSource.addProbe(probe);  // this does registerProbe and activateProbe
        }

        // and connect
        dataSource.connect();

        // turn on probes
        for (RouterProbe probe : probeList) {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          leadin() + " data source: " +  dataSource.getName() + " turn on probe " +
                                          probe.getName());

            dataSource.turnOnProbe(probe);

            probe.started();
        }

    }

    /**
     * Pause monitoring
     */
    public synchronized void pauseMonitoring() {
        for (RouterProbe probe : probeList) {
            if (dataSource.isProbeOn(probe)) {
                dataSource.turnOffProbe(probe);

                probe.paused();
            }
        }
    }

    /**
     * Resume monitoring
     */
    public synchronized void resumeMonitoring() {
        for (RouterProbe probe : probeList) {
            if (!dataSource.isProbeOn(probe)) {
                dataSource.turnOnProbe(probe);

                probe.resumed();
            }
        }
    }

    /**
     * Stop monitoring.
     */
    public synchronized void stopMonitoring() {
        if (dataSource.isConnected()) {
            //pauseMonitoring();

            // turn off probes
            for (RouterProbe probe : probeList) {
                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + " data source: " +  dataSource.getName() + " turn off probe " +
                                              probe.getName());

                probe.lastMeasurement();

                dataSource.turnOffProbe(probe);

            }

            // disconnect
            dataSource.disconnect();

            // remove probes
            for (RouterProbe probe : probeList) {
                dataSource.removeProbe(probe);
            }

        }
    }

    /**
     * Set up the probes
     */
    private void setupProbes(HashMap<String, Integer> probeInfoMap) {
        // skip through the map, instantiate a Probe and set its data rate
        for (Map.Entry<String, Integer> entry : probeInfoMap.entrySet()) {
            String probeClassName = entry.getKey();
            Integer datarate = entry.getValue();

            try {
                // Now convert the class name to a Class
                // get Class object

                // WAS Class<RouterProbe> cc = (Class<RouterProbe>)Class.forName(probeClassName);

                // Replaced with following 2 lines
                Class<?> c = (Class<?> )Class.forName(probeClassName);
                Class<? extends RouterProbe> cc = c.asSubclass(RouterProbe.class );

                // find Constructor for when arg is RouterController
                Constructor<? extends RouterProbe> cons = (Constructor<? extends RouterProbe> )cc.getDeclaredConstructor(
                        RouterController.class);

                RouterProbe probe = (RouterProbe)cons.newInstance(this);

                // Set datarate, iff we need to
                if (datarate > 0) {
                    probe.setDataRate(new EveryNMilliseconds(datarate));
                }

                probeList.add(probe);

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Added probe: " + probe);

            } catch (ClassNotFoundException cnfe) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Class not found " + probeClassName);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin() + "Cannot instantiate class " + probeClassName + " because " +
                                              e.getMessage());
                e.printStackTrace();
            }


        }
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RC = "RC: ";

        return getName() + " " + RC;
    }

}
