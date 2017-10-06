package usr.router;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;

import usr.APcontroller.APController;
import usr.APcontroller.APInfo;
import usr.APcontroller.ConstructAPController;
import usr.applications.ApplicationHandle;
import usr.applications.ApplicationManager;
import usr.applications.ApplicationResponse;
import usr.console.ComponentController;
import usr.common.TimedThread;
import usr.common.SimpleThreadFactory;
import usr.common.ANSI;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import cc.clayman.console.ManagementConsole;
import eu.reservoir.monitoring.appl.BasicDataSource;
import eu.reservoir.monitoring.appl.datarate.EveryNMilliseconds;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneProducerWithNames;


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
    int newConnectionPort;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    // have we been informed of a failure
    boolean failureNotification = false;
    Object failureNotificationSyncObj = new Object();
    
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

    // Executor Service to start new apps
    ExecutorService executorService;

    // The ThreadGroup
    ThreadGroup threadGroup;

    // Information about the APcontroller
    APController apController_ = null;
    APInfo apInfo_ = null;


    AP apMgr;   // An Agg Point Manager

    RouterOptions options_ = null;

    // Map of NetIFs that are in the process of being finalized
    // and are temporarily held here in the RouterController.
    HashMap<Integer, NetIF> tempNetIFMap;

    // A BasicDataSource for the stats of a Router
    BasicDataSource dataSource = null;

    // The probes
    ArrayList<RouterProbe> probeList = null;

    CountDownLatch latch = null;
    NetIF latchForNetIF = null;


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
        this.newConnectionPort = r2rPort;

        // delegate listening of commands to a ManagementConsole object
        management = new RouterManagementConsole(this, mPort);

        // default connections
        connections = new RouterConnectionsTCP(this, newConnectionPort);

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
        dataSource = new RouterDataSource(name + ".dataSource");
        probeList = new ArrayList<RouterProbe>();

        System.out.println(leadin() + "Setup DataSource: " + dataSource);
    }

    /**
     * Get the name of this RouterController.
     */
    @Override
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
    @Override
    public ManagementConsole getManagementConsole() {
        return management;
    }

    protected RouterConnections getRouterConnections() {
        return connections;
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
        myThread = new TimedThread(threadGroup, this, "/" + router.getName() + "/RouterController/" + hashCode());
        running = true;
        myThread.start();

        // start router to router connections listener
        boolean startedC = connections.start();

        // start management console listener
        boolean startedL = management.start();

        // start appSocketMux
        appSocketMux = new AppSocketMux(this);
        appSocketMux.start();


        // executorService used to start Apps in their own thread
        executorService = Executors.newSingleThreadExecutor(new SimpleThreadFactory(getThreadGroup(), "AppStart-" + router.getName()));



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


        // shutdown executorService used to start Apps in their own thread
        executorService.shutdown();

        // stop applications
        stopApplications();

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
     * A function used by the RouterConnectionsXXX objects
     * to tell the RouterController there is a kind of failure.
     */
    void informFailure() {
        synchronized (failureNotificationSyncObj) {
            failureNotification = true;
            failureNotificationSyncObj.notify();
        }
    }

    /**
     * The main thread loop.
     */
    @Override
    public void run() {
        while (failureNotification == false && running == true) {
            synchronized (failureNotificationSyncObj) {
                try {
                    failureNotificationSyncObj.wait();
                } catch (InterruptedException ie) {
                    if (running == false) {
                        // We might get an informFailure() during a stop()
                        //System.err.println(ANSI.YELLOW + "Interrupted on stop()" + ANSI.RESET_COLOUR);
                    } else {
                        System.err.println(ANSI.RED + "Interrupted on informFailure()" + ANSI.RESET_COLOUR);
                    }
                }
            }
        }
    }

    /** Return a routing table as a string */
    public RoutingTable getRoutingTable() {
        return router.getRoutingTable();
    }

    /**
     * Keep a handle on a NetIF created from INCOMING_CONNECTION
     */
    void registerTemporaryNetIFIncoming(NetIF netIF) {
        int id = netIF.getID();

        latch = new CountDownLatch(1);
        latchForNetIF = netIF;

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + ANSI.CYAN + "LATCH SET for id: " + id + ANSI.RESET_COLOUR);

        registerTemporaryNetIF(netIF);

        try {
            latch.await();
        } catch (InterruptedException ie) {
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() +  ANSI.CYAN + "LATCH FREE for id: " + id + ANSI.RESET_COLOUR);


    }

    /**
     * Find a NetIF by an id.
     */
    public synchronized NetIF getTemporaryNetIFByID(int id) {
        if (latch != null && latchForNetIF.getID() == id) {
            // reduce latch count by 1
            latch.countDown();

            Logger.getLogger("log").logln(USR.STDOUT, leadin() +  ANSI.CYAN + "LATCH DOWN for id:" + id + ANSI.RESET_COLOUR);
            return tempNetIFMap.get(id);
        } else {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() +  ANSI.RED + "CANT FIND NetIF id:" + id + ANSI.RESET_COLOUR);
            return null;
        }
    }

    /**
     * Keep a handle on a NetIF created from INCOMING_CONNECTION
     */
    void registerTemporaryNetIF(NetIF netIF) {
        int id = netIF.getID();

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "temporary addNetIF ID: " + id + "address: " + netIF.getAddress() + " for " + netIF);

        synchronized (tempNetIFMap) {
            tempNetIFMap.put(id, netIF);
        }

    }

    /**
     * List all the temporary connections held by the Controller
     */
    Collection<NetIF> listTempNetIF() {
        synchronized (tempNetIFMap) {
            return tempNetIFMap.values();
        }
    }

    /**
     * Plug a NetIF into the RouterFabric
     */
    public RouterPort plugTemporaryNetIFIntoPort(NetIF netIF) {
        synchronized (tempNetIFMap) {
            RouterPort rp = plugInNetIF(netIF);
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "plugInNetIF "  + netIF);

            tempNetIFMap.remove(netIF.getID());

            return rp;
        }
    }

    /**
     * Plug in a NetIF to the Router.
     */
    public RouterPort plugInNetIF(NetIF netIF) {
        RouterFabric fabric = router.getRouterFabric();
        RouterPort rp = fabric.addNetIF(netIF);
        netIF.setRouterPort(rp);

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
    public synchronized ApplicationResponse appStart(String command) {
        // This starts an app within the context of the Router's thread groups
        // rather than the context of the caller
        //

        final String commandstr = command;

        // We create a Callable to enable this
        // The call() gets the ApplicationManager to start the App
        Callable<ApplicationResponse> callable = new Callable<ApplicationResponse>() {
                public ApplicationResponse call() throws Exception {
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
            };

        try {
            // Start Callable, and wait for the response
            Future future = executorService.submit(callable);

            // Get the response
            ApplicationResponse resp = (ApplicationResponse)future.get();

            return resp;
        } catch (ExecutionException ee) {
            return new ApplicationResponse(false, ee.getMessage());
        } catch (InterruptedException ie) {
            return new ApplicationResponse(false, ie.getMessage());
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

        // delegate listening for new connections to RouterConnections object


        RouterOptions.LinkType linkType = options_.getLinkType();

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "LinkType = " + linkType);
        
        if (linkType == RouterOptions.LinkType.UDP) {
            connections = new RouterConnectionsUDP(this, newConnectionPort);
        } else {
            connections = new RouterConnectionsTCP(this, newConnectionPort);
        }

        // Setup AP
        String apClassName = options_.getAPClassName();

        if (apClassName == null) {
            // there is no AP defined in the options
            // use the built-in one
            apClassName = "usr.router.NullAPCreator";
        }

        setupAP(apClassName);  //         apMgr = new AggPointCreator(this);


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

    RouterOptions getRouterOptions() {
        return options_;
    }

    /** Set the aggregation point for this router */
    public synchronized void setAP(int gid, int ap, String[] ctxArgs) {
        apMgr.setAP(gid, ap, ctxArgs);
    }


    /**
     * Set up the AP
     */
    private void setupAP(String apClassName) {
        try {
            Class<?> c = Class.forName(apClassName);
            Class<? extends AP> cc = c.asSubclass(AP.class);

            // find Constructor for when arg is RouterController
            Constructor<? extends AP> cons = cc.getDeclaredConstructor(RouterController.class);

            apMgr = cons.newInstance(this);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Setup AP: " + apMgr);

        } catch (ClassNotFoundException cnfe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Class not found " + apClassName);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot instantiate class " + apClassName);
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

                //probe.lastMeasurement();

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
                // Ger class info
                Class<?> c = Class.forName(probeClassName);
                Class<? extends RouterProbe> cc = c.asSubclass(RouterProbe.class );

                // find Constructor for when arg is RouterController
                Constructor<? extends RouterProbe> cons = cc.getDeclaredConstructor(RouterController.class);

                RouterProbe probe = cons.newInstance(this);

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


class RouterDataSource extends BasicDataSource {
    public RouterDataSource(String s) {
        super(s);
    }

    /**
     * Receive a measurement from the Probe
     * and pass it onto the data source delegate.
     * @return -1 if something goes wrong
     * @return 0 if there is no delegate or no data plane
     */
    public int notifyMeasurement(eu.reservoir.monitoring.core.Measurement m) {
	//System.err.println("DataSource: " + name + ": " + m);
        Logger.getLogger("log").logln(1<<6, "> " +  m.getType() + "." + m.getProbeID() + "." + m.getSequenceNo());
        return super.notifyMeasurement(m);
    }
}
