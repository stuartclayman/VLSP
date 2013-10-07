package usr.globalcontroller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.APcontroller.APController;
import usr.APcontroller.ConstructAPController;
import usr.abstractnetwork.AbstractLink;
import usr.abstractnetwork.AbstractNetwork;
import usr.common.BasicRouterInfo;
import usr.common.LinkInfo;
import usr.common.LocalHostInfo;
import usr.common.Pair;
import usr.common.PortPool;
import usr.common.ProcessWrapper;
import usr.console.ComponentController;
import usr.engine.EventEngine;
import usr.engine.ProbabilisticEventEngine;
import usr.events.AppStartEvent;
import usr.events.EndLinkEvent;
import usr.events.EndRouterEvent;
import usr.events.Event;
import usr.events.EventScheduler;
import usr.events.NetStatsEvent;
import usr.events.OutputEvent;
import usr.events.SetAggPointEvent;
import usr.events.SimpleEventScheduler;
import usr.events.StartLinkEvent;
import usr.interactor.LocalControllerInteractor;
import usr.lifeEstimate.LifetimeEstimate;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import usr.output.OutputTraffic;
import usr.output.OutputType;
import usr.router.RouterOptions;
import cc.clayman.console.ManagementConsole;
import eu.reservoir.monitoring.appl.BasicConsumer;
import eu.reservoir.monitoring.appl.BasicDataSource;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.Reporter;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneForwardingConsumerWithNames;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneProducerWithNames;

/**
 * The GlobalController is in overall control of the software.  It
 * contacts LocalControllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
public class GlobalController implements ComponentController {
    private ControlOptions options_;      // Options affecting the simulation

    // Options structure which is given to each router.
    private RouterOptions routerOptions_ = null;

    private String xmlFile_;              // name of XML file containing config
    private LocalHostInfo myHostInfo_;    // Information about the localhosts
    private GlobalControllerManagementConsole console_ = null;
    private ArrayList<LocalControllerInteractor> localControllers_
        = null;
    private HashMap<String,
                    ProcessWrapper> childProcessWrappers_ = null;
    private ArrayList<String> childNames_ = null;

    // names of child processes

    private AbstractNetwork network_ = null;

    // Map connections LocalControllerInfo for given LCs to the appropriate interactors
    private HashMap<LocalControllerInfo, LocalControllerInteractor> interactorMap_ = null;

    // Map is used to store vacant ports on local controllers
    private HashMap<LocalControllerInfo, PortPool> portPools_ = null;

    // Map is from router Id to information one which machine router is
    // stored on.
    private ConcurrentHashMap<Integer, BasicRouterInfo> routerIdMap_ = null;

    // A list of all the routers that have been shutdown
    private ArrayList<BasicRouterInfo> shutdownRouters_ = null;

    // A map of routerID links to LinkInfo objects
    private HashMap<Integer, LinkInfo> linkInfo = null;

    // A Map if appID to routerID
    // i.e the router the app is running on
    private HashMap<Integer, Integer> appInfo = null;

    // A list of agg points
    private ArrayList<Integer> apList = null;
    // A map of routerID to the agg point for that router
    private HashMap<Integer, Integer> apInfo = null;

    private int aliveCount = 0;   // Counts number of live nodes running.

    private EventScheduler scheduler_ = null;    // Class holds scheduler for event list


    // Variables relate to traffic output of statistics
    private ArrayList<OutputType> trafficOutputRequests_ = null;
    private String routerStats_ = "";
    private int statsCount_ = 0;
    private ArrayList<Long> trafficOutputTime_ = null;
    private HashMap<String, int []> trafficLinkCounts_ = null;

    private ArrayList<OutputType> eventOutput_ = null;

    // Number of aggregation point controllers
    private int noControllers_ = 0;

    // Thread name
    private String myName = "GlobalController";

    // Controller assigns aggregation points
    private APController APController_ = null;

    // Used in shut down routines
    private boolean isActive_ = false;

    // Object used in emulation simply to wait
    private Object runLoop_;

    // Doing Lattice monitoring ?
    boolean latticeMonitoring = false;

    // A monitoring address
    InetSocketAddress monitoringAddress;
    int monitoringPort = 22997;
    int monitoringTimeout = 1;

    // Forwarding address
    InetSocketAddress forwardAddress;

    // A BasicConsumer for the stats of a Router
    BasicConsumer dataConsumer;

    // and the Reporters that handle the incoming measurements
    // Label -> Reporter
    HashMap<String, Reporter> reporterMap;

    // A BasicDataSource for the lifecycle of a Router
    BasicDataSource dataSource = null;

    // The probes
    ArrayList<Probe> probeList = null;

    // A Semaphore to have single access to some operations
    Semaphore semaphore;

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Command line must specify "
                               + "XML file to read and nothing else.");
            System.exit(-1);
        }

        try {
            GlobalController gControl = new GlobalController();

            if (args.length > 1) {
                gControl.setStartupFile(args[1]);
                gControl.init();
            } else {
                gControl.setStartupFile(args[0]);
                gControl.init();
            }

            gControl.start();


            Logger.getLogger("log").logln(USR.STDOUT, gControl.leadin() + "Simulation complete");
            System.out.flush();

        } catch (Throwable t) {
            System.exit(1);
        }
        /*
        GlobalController gControl = new GlobalController();
        gControl.xmlFile_ = args[0];
        gControl.init();
        Logger.getLogger("log").logln(USR.STDOUT, gControl.leadin() + "Global controller session complete");
        System.out.flush();
        */
    }

    /**
     * Construct a GlobalController -- this constructor contains things
     * which apply whether we are simulation or emulation
     */
    public GlobalController() {
    }

    /** Basic intialisation for the global controller */
    protected void init() {
        // allocate a new logger
        Logger logger = Logger.getLogger("log");

        // tell it to output to stdout and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.STDOUT set

        // tell it to output to stderr and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.ERROR set
        logger.addOutput(System.err, new BitMask(USR.ERROR));
        logger.addOutput(System.out, new BitMask(USR.STDOUT));

        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Hello");

        network_ = new AbstractNetwork();
        shutdownRouters_ = new ArrayList<BasicRouterInfo>();
        linkInfo = new HashMap<Integer, LinkInfo>();
        appInfo = new HashMap<Integer, Integer>();
        apList = new ArrayList<Integer>();
        apInfo = new HashMap<Integer, Integer>();
        options_ = new ControlOptions(xmlFile_);
        routerOptions_ = options_.getRouterOptions();

        eventOutput_ = options_.getEventOutput();

        // create a semaphore with 1 element to ensure
        // single access to some key code blocks
        semaphore = new Semaphore(1);

        runLoop_ = new Object();

        // Redirect ouptut for error and normal output if requested in
        // router options file
        String fileName = routerOptions_.getOutputFile();

        if (!fileName.equals("")) {
            if (routerOptions_.getOutputFileAddName()) {
                fileName += "_" + leadinFname();
            }

            File output = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output, true);
                PrintWriter pw = new PrintWriter(fos, true);
                logger.removeOutput(System.out);
                logger.addOutput(pw, new BitMask(USR.STDOUT));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.err.println(fileName);
                System.exit(-1);
            }
        }

        String errorName = routerOptions_.getErrorFile();

        if (!errorName.equals("")) {
            if (routerOptions_.getOutputFileAddName()) {
                errorName += "_" + leadinFname();
            }

            File output = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output, true);
                PrintWriter pw = new PrintWriter(fos, true);
                logger.removeOutput(System.err);
                logger.addOutput(pw, new BitMask(USR.ERROR));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.err.println(fileName);
                System.exit(-1);
            }
        }

        // Set up AP controller
        APController_ = ConstructAPController.constructAPController(routerOptions_);

        try {
            myHostInfo_ = new LocalHostInfo(options_.getGlobalPort());
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            System.exit(-1);
        }

        if (options_.latticeMonitoring()) {
            latticeMonitoring = true;
        }

        if (latticeMonitoring) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting monitoring");

            // setup DataConsumer
            dataConsumer = new BasicConsumer();

            // and reporterList
            reporterMap = new HashMap<String, Reporter>();

            // now start the reporters
            setupReporters(options_.getConsumerInfo());

            // start monitoring
            // listening on the GlobalController address
            String gcAddress = "localhost";

            try {
                gcAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException uhe) {
            }

            monitoringAddress = new InetSocketAddress(gcAddress, monitoringPort);

            Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Starting monitoring on " + monitoringAddress);

            // Forwarding address
            forwardAddress = new InetSocketAddress(gcAddress, monitoringPort + 1);

            startMonitoringConsumer(monitoringAddress);


            /* set up probes to send out data */

            // setup DataSource
            dataSource = new BasicDataSource(getName() + ".dataSource");
            probeList = new ArrayList<Probe>();


            startMonitoringProducer(forwardAddress);
        }

        // Set up specific details if this is actually emulation not
        // simulation
        if (!options_.isSimulation()) {
            initEmulation();
        }

        //Initialise events for schedules
        scheduler_ = new SimpleEventScheduler(options_.isSimulation(), this);
        options_.initialEvents(scheduler_, this);

        // Clear output files where needed
        for (OutputType o : options_.getOutputs()) {
            if (o.clearOutputFile()) {
                File f = new File(o.getFileName());
                f.delete();
            }
        }

        if (options_.getWarmUpPeriod() > 0) {
            for (EventEngine e : options_.getEngines()) {
                if (e instanceof ProbabilisticEventEngine) {
                    ((ProbabilisticEventEngine)e).warmUp(
                        options_.getWarmUpPeriod(), APController_, this);
                }
            }
        }

    }

    public void start() {
        if (options_.isSimulation()) {
            runSimulation();
        } else {
            runEmulation();
        }
    }

    /** Runs a simulation loop --- gets events and executes them in order.
     */

    private void runSimulation() {
        isActive_ = true;
        while (isActive_) {
            Event ev = scheduler_.getFirstEvent();

            if (ev == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Ran out of events to schedule");
                break;
            }

            try {
                executeEvent(ev);
            } catch (InstantiationException ine) {
            	Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not instantiate"+ine);
                isActive_ = false;
            } catch (InterruptedException ie) {
            	Logger.getLogger("log").logln(USR.ERROR, leadin() + "Interrupted event"+ie);
                isActive_ = false;
            } catch (TimeoutException te) {
            	Logger.getLogger("log").logln(USR.ERROR, leadin() + "Event got timeout"+te);
                isActive_ = false;
            }
        }

        shutDown();
    }

    /** Runs an emulation loop -- this spawns the scheduler as an independent
     *  process then waits.  The scheduler sends events back into the  main loop
     */

    private void runEmulation() {
        isActive_ = true;
        // Start Scheduler as thread
        Thread t;
        synchronized (runLoop_) {
            t = new Thread(scheduler_);
            t.start();

            while (isActive_) {
                try {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "runLoop_ wait");
                    runLoop_.wait();
                } catch (InterruptedException ie) {
                } catch (IllegalMonitorStateException ims) {
                }
            }
        }


        scheduler_.wakeWait(); // Interrupt the scheduler to close it

        if (t.isAlive()) {
            scheduler_.wakeWait();
            try {
                t.join();
            } catch (InterruptedException ie) {
            }
        }

        shutDown();
    }

    /** Sets isActive_ to false ending the simulation */
    public void deactivate() {
        isActive_ = false;

        if (!options_.isSimulation()) {
            synchronized (runLoop_) {
                runLoop_.notify();
            }
        }
    }

    /**
     * Initialisation if we are emulating on hardware.
     */
    private void initEmulation() {
        childProcessWrappers_ = new HashMap<String, ProcessWrapper>();
        childNames_ = new ArrayList<String>();
        routerIdMap_ = new ConcurrentHashMap<Integer, BasicRouterInfo>();
        startConsole();
        portPools_ = new HashMap<LocalControllerInfo, PortPool>();
        noControllers_ = options_.noControllers();
        LocalControllerInfo lh;

        for (int i = 0; i < noControllers_; i++) {
            lh = options_.getController(i);
            portPools_.put(lh,
                           new PortPool(lh.getLowPort(), lh.getHighPort()));
        }

        if (options_.startLocalControllers()) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting Local Controllers");
            startLocalControllers();
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Checking existence of local Controllers");
        checkAllControllers();
    }

    /**
     * Set up the reporters
     */
    private void setupReporters(HashMap<String,
                                        String> reporterInfoMap) {
        // skip through the map, instantiate a Probe and set its data
        // rate
        for (Map.Entry<String,
                       String> entry : reporterInfoMap.entrySet()) {
            String reporterClassName = entry.getValue();
            String label = entry.getKey();

            try {
                // Now convert the class name to a Class
                // get Class object
                // WAS Class<Reporter> cc =
                // (Class<Reporter>)Class.forName(reporterClassName);

                // Replaced with following 2 lines
                Class<?> c =
                    Class.forName(reporterClassName);
                Class<? extends Reporter> cc = c.asSubclass(
                        Reporter.class);

                // find Constructor for when arg is GlobalController
                Constructor<? extends Reporter> cons
                    = cc.
				    getDeclaredConstructor(GlobalController.class);

                Reporter reporter = cons.newInstance(this);
                reporterMap.put(label, reporter);

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Added reporter: " + reporter);
            } catch (ClassNotFoundException cnfe) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Class not found " + reporterClassName);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot instantiate class " + reporterClassName);
            }
        }
    }

    /**
     * Schedule the creation of a link between two nodes
     * @param node1 first node
     * @param node2 second node
     */
    public void scheduleLink(AbstractLink link, EventEngine eng, long time)
    {
        StartLinkEvent sle= new StartLinkEvent(time, eng, link);
        scheduler_.addEvent(sle);
        network_.scheduleLink(link);
    }

    /** checks if events can be simulated */
    public boolean isActive() {
        return isActive_;
    }

    /**
     * Start the console.
     */
    protected void startConsole() {
        console_ = new GlobalControllerManagementConsole(this, myHostInfo_.getPort());
        console_.start();
    }

    /**
     * Stop the console.
     */
    protected void stopConsole() {
        console_.stop();
    }


    /** Do an Operation protected by a Semaphore.
     * Return a JSON object with information about it
     * throws InstantiationException if creation fails
     * InterruptedException if acquisition of lock interrupted
     * TimeoutException if acquisition timesout*/
    public JSONObject semaphoredOperation(Operation op) throws Exception {
        try {
            // Wait to aquire a lock -- only one event at once
            //
            boolean acquired = semaphore.tryAcquire(options_.getMaxLag(), TimeUnit.MILLISECONDS);

            if (!acquired) {
                throw new TimeoutException(leadin()+"GlobalController lagging too much");
            }

            if (!isActive_) {
                throw new InterruptedException("Run finished!");
            }

            JSONObject js = null;

            js = op.call();

            return js;
        } finally {
            semaphore.release();
        }
    }

    /** Execute an event,
     * return a JSON object with information about it
     * throws Instantiation if creation fails
     * Interrupted if acquisition of lock interrupted
     * Timeout if acquisition timesout
     */
    public JSONObject executeEvent(Event ev) throws InstantiationException, InterruptedException, TimeoutException {

        try {
            // Get a 'final' handle on the Event
            final Event e = ev;
            // Get a 'final' handle on the GlobalController
            final GlobalController gc = this;

            // Define the Operation body
            // the method 'call()' is called by semaphoredOperation()
            Operation execute = new Operation() {
                    @Override
					public JSONObject call() throws InstantiationException, InterruptedException, TimeoutException {
                    	Logger.getLogger("log").logln(USR.STDOUT, "EVENT preceed: " + e);
                    	e.preceedEvent(scheduler_, gc);
                        Logger.getLogger("log").logln(USR.STDOUT, "EVENT execute: " + e);
                        JSONObject js = null;
                        js = e.execute(gc);

                        Logger.getLogger("log").logln(USR.STDOUT, "EVENT result:  " + js);

                        for (OutputType t : eventOutput_) {
                            produceEventOutput(e, js, t);
                        }
                        Logger.getLogger("log").logln(USR.STDOUT, "EVENT follow: " + e);

                        e.followEvent(scheduler_, js, gc);
                        Logger.getLogger("log").logln(USR.STDOUT, "EVENT done: " + e);
                        return js;
                    }
                };

            // Do the Operation in the semaphore code block
            JSONObject jsobj = semaphoredOperation(execute);
            return jsobj;
        } catch (Exception ex) {
            throw new InstantiationException(ex.getMessage());
        }
    }

    /** Convenience function to create JSON object from error string*/
    static public JSONObject commandError(String error) {
        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("msg", "ERROR: " + error);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "JSON creation error in commandError");
        }

        return jsobj;
    }

    /** Event for start Simulation */
    public void startSimulation(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Start of simulation event at: "
                                      + time + " " + System.currentTimeMillis());

        for (OutputType o : options_.getOutputs()) {
            if (o.getTimeType() == OutputType.AT_START) {
                produceOutput(time, o);
            }
        }
    }

    /** Event for end Simulation */
    public void endSimulation(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "End of simulation event at " + time
                                      + " " + System.currentTimeMillis());

        for (OutputType o : options_.getOutputs()) {
            if (o.getTimeType() == OutputType.AT_END) {
                produceOutput(time, o);
            }
        }

        shutDown();
    }


    /** Return the local controller attached to a router id*/
    public LocalControllerInteractor getLocalController(int rId) {
        BasicRouterInfo br = findRouterInfo(rId);

        if (br == null) {
            return null;
        }

        return interactorMap_.get(br.getLocalControllerInfo());
    }

    /** Return the local controller attached to router info*/
    public LocalControllerInteractor getLocalController(BasicRouterInfo br) {
        return interactorMap_.get(br.getLocalControllerInfo());
    }

    /** Return the local controller attached to router info*/
    public LocalControllerInteractor getLocalController(LocalControllerInfo lcinf) {
        return interactorMap_.get(lcinf);
    }

    public LocalControllerInfo getLeastUsedLC() {
        LocalControllerInfo leastUsed = options_.getController(0);

        double minUse = leastUsed.getUsage();
        double thisUsage;

        for (int i = 1; i < noControllers_; i++) {
            LocalControllerInfo lc = options_.getController(i);
            thisUsage = lc.getUsage();

            // Logger.getLogger("log").logln(USR.STDOUT, i+" Usage "+thisUsage);
            if (thisUsage == 0.0) {
                leastUsed = lc;
                break;
            }

            if (thisUsage < minUse) {
                minUse = thisUsage;
                leastUsed = lc;
            }
        }

        if (minUse >= 1.0) {
            return null;
        }

        return leastUsed;
    }

    /** Get the port pool associated with a local controller */
    public PortPool getPortPool(LocalControllerInfo lci) {
        return portPools_.get(lci);
    }



    /** Register existence of router */
    public void registerRouter(int rId) {
        network_.addNode(rId);
        // inform about all routers
        informAllRouters();

    }

    /** Unregister a router and all links from structures in
     *  GlobalController*/
    public void unregisterRouter(int rId, long time) {
        int[] out = getOutLinks(rId);
        APController_.removeNode(getElapsedTime(), rId);
        for (int i = out.length - 1; i >= 0; i--) {
            unregisterLink(rId, out[i]);
        }

        network_.removeNode(rId);

        // inform about all routers
        informAllRouters();


    }

    /**
     * End router
     */
    public JSONObject sendRouter(int routerId) {
        try {
            EndRouterEvent ev = new EndRouterEvent(getElapsedTime(), null, routerId);
            JSONObject jsobj = executeEvent(ev);
            return jsobj;
        } catch (Exception e) {
            return null;
        }
    }

    /** Find some router info
     */
    public BasicRouterInfo findRouterInfo(int rId) {

        return routerIdMap_.get(rId);
    }

    /**
     * Find some router info, given a router address or a router name
     * and return a JSONObject
     */
    public JSONObject findRouterInfoAsJSON(int routerID) throws JSONException {
        BasicRouterInfo bri = findRouterInfo(routerID);

        JSONObject jsobj = new JSONObject();

        jsobj.put("time", bri.getTime());
        jsobj.put("routerID", bri.getId());
        jsobj.put("name", bri.getName());
        jsobj.put("address", bri.getAddress());
        jsobj.put("mgmtPort", bri.getManagementPort());
        jsobj.put("r2rPort", bri.getRoutingPort());

        // now get all outlinks
        JSONArray outArr = new JSONArray();
        int [] outLinks = getOutLinks(routerID);

        for (int outLink : outLinks) {
            outArr.put(outLink);
        }

        jsobj.put("links", outArr);

        return jsobj;

    }

    /** add some router info */
    public void addRouterInfo(int id, BasicRouterInfo br) {
        routerIdMap_.put(id, br);

        informRouterStarted(br.getName());
    }

    /** remove some router info*/
    public void removeRouterInfo(int rId) {
        BasicRouterInfo rInfo = findRouterInfo(rId);

        // remove all LinkInfo objects that refer to this router.
        Collection<LinkInfo> links = findLinkInfoByRouter(rId);

        for (LinkInfo lInfo : links) {
            linkInfo.remove(lInfo.getLinkID());
        }

        // remove agg point info
        if (apList.contains(rId)) {
            apList.remove(Integer.valueOf(rId)); // need to pass in object
        }
        apInfo.remove(rId);

        // remove router from BasicRouterInfo map
        routerIdMap_.remove(rId);

        // now add it to shutdown routers
        shutdownRouters_.add(new BasicRouterInfo(rId, getTime()));

        // inform anyone that a Router has Ended
        informRouterEnded(rInfo.getName());

    }

    /**
     * Called after a router is started.
     */
    protected void informRouterStarted(String name) {
        // tell reporter that this router is created
        if (latticeMonitoring) {
            String routerName = name;

            // tell all Reporters thar router is deleted
            for (Reporter reporter : reporterMap.values()) {
                if (reporter instanceof RouterCreatedNotification) {
                    ((RouterCreatedNotification)reporter).routerCreated(routerName);
                }
            }

            // tell all Probes thar router is created
            for (Probe probe : probeList) {
                if (probe instanceof RouterCreatedNotification) {
                    ((RouterCreatedNotification)probe).routerCreated(routerName);
                }
            }
        }

    }

    /**
     * Called after a router is ended.
     */
    protected void informRouterEnded(String name)  {
        // tell reporter that this router is gone
        if (latticeMonitoring) {
            String routerName = name;

            // tell all Reporters thar router is deleted
            for (Reporter reporter : reporterMap.values()) {
                if (reporter instanceof RouterDeletedNotification) {
                    ((RouterDeletedNotification)reporter).routerDeleted(routerName);
                }
            }

            // tell all Probes thar router is deleted
            for (Probe probe : probeList) {
                if (probe instanceof RouterDeletedNotification) {
                    ((RouterDeletedNotification)probe).routerDeleted(routerName);
                }
            }
        }
    }

    /**
     * Called to give a snapshot of all the routers
     */
    protected void informAllRouters() {
    }

    /**
     * Called to give a snapshot of all the links
     */
    protected void informAllLinks() {
    }


    /**
     * Find some router info, given a router address or a router name
     */
    public BasicRouterInfo findRouterInfo(String value) {
        // skip through all the BasicRouterInfo objects
        for (BasicRouterInfo info : routerIdMap_.values()) {
            if (info.getAddress() !=
                null &&info.getAddress().equals(value)) {
                // we found a match
                return info;
            } else if (info.getName() !=
                       null &&info.getName().equals(value)) {
                // we found a match
                return info;
            }
        }

        // we got here and found nothing
        return null;
    }

    /**
     * List all RouterInfo.
     */
    public Collection<BasicRouterInfo> getAllRouterInfo() {
        return routerIdMap_.values();
    }

    /**
     * List all RouterInfo as a JSON object
     */
    public JSONObject getAllRouterInfoAsJSON(String detail) throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray detailArray = new JSONArray();

        for (BasicRouterInfo info : getAllRouterInfo()) {
            int routerID = info.getId();

            array.put(routerID);

            if (detail.equals("all")) {
                // add a detailed record
                JSONObject record = new JSONObject();
                record.put("time", info.getTime());
                record.put("routerID", info.getId());
                record.put("name", info.getName());
                record.put("address", info.getAddress());
                record.put("mgmtPort", info.getManagementPort());
                record.put("r2rPort", info.getRoutingPort());

                // now get all outlinks
                JSONArray outArr = new JSONArray();
                int [] outLinks = getOutLinks(routerID);

                for (int outLink : outLinks) {
                    outArr.put(outLink);
                }

                record.put("links", outArr);



                detailArray.put(record);

            }
        }

        jsobj.put("type", "router");
        jsobj.put("list", array);

        if (detail.equals("all")) {
            jsobj.put("detail", detailArray);
        }

        return jsobj;
    }

    /**
     * Get the number of routers
     */
    public int getRouterCount() {
        return network_.getNoNodes();
    }

    /**
     * Is the router ID valid.
     */
    public boolean isValidRouterID(int rId) {
        return network_.nodeExists(rId);
    }

    /**
     * List all shutdown routers
     */
    public ArrayList<BasicRouterInfo> getShutdownRouters() {
        return shutdownRouters_;
    }

    /**
     * List all shutdown routers
     */
    public JSONObject listShutdownRoutersAsJSON() throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();

        for (BasicRouterInfo info : getShutdownRouters()) {
            JSONObject obj = new JSONObject();
            obj.put("routerID", info.getId());
            obj.put("time", info.getTime());

            array.put(obj);
        }

        jsobj.put("type", "shutdown");
        jsobj.put("list", array);

        return jsobj;
    }


    /**
     * Start a link
     */
    public JSONObject startLink(int r1, int r2, int weight) {
        try {
            System.err.println("Start link object JSON "+r1+" "+r2);
            StartLinkEvent ev = new StartLinkEvent(getElapsedTime(), null, r1, r2, weight);
            JSONObject jsobj = executeEvent(ev);
            return jsobj;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * End a link
     */
    public JSONObject endLink(int r1, int r2, int weight) {
        try {
            EndLinkEvent ev = new EndLinkEvent(getElapsedTime(), null, r1, r2);
            JSONObject jsobj = executeEvent(ev);
            return jsobj;
        } catch (Exception e) {
            return null;
        }

    }


    /**
     * Find link info
     */
    public LinkInfo findLinkInfo(int linkID) {
        return linkInfo.get(linkID);
    }

    /**
     * Set LinkInfo
     */
    public void setLinkInfo(Integer linkID, LinkInfo linkinf) {
        linkInfo.put(linkID, linkinf);
    }

    /**
     * Find link info
     * and return a JSONObject
     */
    public JSONObject findLinkInfoAsJSON(int linkID) throws JSONException {
        LinkInfo li = findLinkInfo(linkID);

        JSONObject jsobj = new JSONObject();

        jsobj.put("time", li.getTime());
        jsobj.put("linkID", li.getLinkID());
        jsobj.put("linkName", li.getLinkName());
        jsobj.put("weight", li.getLinkWeight());
        // now get connected nodes
        JSONArray nodes = new JSONArray();

        nodes.put(li.getEndPoints().getFirst());
        nodes.put(li.getEndPoints().getSecond());

        jsobj.put("nodes", nodes);

        return jsobj;
    }

    /**
     * Find links for Router.
     */
    public Collection<LinkInfo> findLinkInfoByRouter(int routerID) {
        ArrayList<LinkInfo> result = new ArrayList<LinkInfo>();

        for (LinkInfo lInfo : linkInfo.values()) {
            Pair<Integer, Integer> routers = lInfo.getEndPoints();

            if (routers.getFirst() == routerID || routers.getSecond() == routerID) {
                // same router
                result.add(lInfo);
            }
        }

        return result;
    }


    /**
     * List all LinkInfo
     */
    public Collection<LinkInfo> getAllLinkInfo() {
        return linkInfo.values();
    }

    /**
     * List all LinkInfo as a JSONObject
     */
    public JSONObject getAllLinkInfoAsJSON(String detail) throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray detailArray = new JSONArray();

        for (LinkInfo info : getAllLinkInfo()) {
            array.put(info.getLinkID());

            if (detail.equals("all")) {
                // add a detailed record
                JSONObject record = new JSONObject();
                record.put("time", info.getTime());
                record.put("id", info.getLinkID());
                record.put("name", info.getLinkName());
                record.put("weight", info.getLinkWeight());

                // now get connected nodes
                JSONArray nodes = new JSONArray();

                nodes.put(info.getEndPoints().getFirst());
                nodes.put(info.getEndPoints().getSecond());

                record.put("nodes", nodes);


                detailArray.put(record);

            }
        }

        jsobj.put("type", "link");
        jsobj.put("list", array);

        if (detail.equals("all")) {
            jsobj.put("detail", detailArray);
        }

        return jsobj;
    }

    /**
     * Is the link ID valid.
     */
    public boolean isValidLinkID(int lId) {
        if (linkInfo.containsKey(lId)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get router stats info, given a router address
     * and return a JSONObject
     */
    public JSONObject getRouterLinkStatsAsJSON(int routerID) throws JSONException {
        // Find the traffic reporter
        // This is done by asking the GlobalController for
        // a class that implements TrafficInfo.
        // It is this class that has the current traffic info.
        TrafficInfo reporter = (TrafficInfo)findByInterface(TrafficInfo.class);


        findLinkInfoByRouter(routerID);

        // result object
        JSONObject jsobj = new JSONObject();

        // array for links
        JSONArray linkArr = new JSONArray();
        // array for stats
        JSONArray statArr = new JSONArray();


        // get outlinks
        int [] outLinks = getOutLinks(routerID);

        for (int outLink : outLinks) {
            // add link
            linkArr.put(outLink);

            // now get link stat info
            String router1Name = findRouterInfo(routerID).getName();
            String router2Name = findRouterInfo(outLink).getName();

            // get trafffic for link i -> j as router1Name => router2Name
            List<Object> iToj = reporter.getTraffic(router1Name, router2Name);

            // now convert list to JSON
            JSONArray linkStats = new JSONArray();

            for (Object obj : iToj) {
                linkStats.put(obj);
            }

            // and add to statArr
            statArr.put(linkStats);

        }

        jsobj.put("type", "link_stats");
        jsobj.put("routerID", routerID);
        jsobj.put("links", linkArr);
        jsobj.put("link_stats", statArr);

        return jsobj;

    }

    /**
     * Get router stats info, given a router address and a destination router
     * and return a JSONObject
     */
    public JSONObject getRouterLinkStatsAsJSON(int routerID, int dstID) throws JSONException {
        // Find the traffic reporter
        // This is done by asking the GlobalController for
        // a class that implements TrafficInfo.
        // It is this class that has the current traffic info.
        TrafficInfo reporter = (TrafficInfo)findByInterface(TrafficInfo.class);


        findLinkInfoByRouter(routerID);

        // result object
        JSONObject jsobj = new JSONObject();

        // array for links
        JSONArray linkArr = new JSONArray();
        // array for stats
        JSONArray statArr = new JSONArray();


        // get outlinks
        int [] outLinks = getOutLinks(routerID);

        for (int outLink : outLinks) {
            if (outLink == dstID) {
                // add link
                linkArr.put(outLink);

                // now get link stat info
                String router1Name = findRouterInfo(routerID).getName();
                String router2Name = findRouterInfo(outLink).getName();

                // get trafffic for link i -> j as router1Name => router2Name
                List<Object> iToj = reporter.getTraffic(router1Name, router2Name);

                // now convert list to JSON
                JSONArray linkStats = new JSONArray();

                for (Object obj : iToj) {
                    linkStats.put(obj);
                }

                // and add to statArr
                statArr.put(linkStats);

            }
        }

        jsobj.put("type", "link_stats");
        jsobj.put("routerID", routerID);
        jsobj.put("links", linkArr);
        jsobj.put("link_stats", statArr);

        return jsobj;

    }


    /** Register a link with structures necessary in Global
     * Controller */
    public void registerLink(int router1Id, int router2Id) {
        network_.addLink(router1Id, router2Id);

        // inform
        informAllRouters();
        informAllLinks();
    }

    /* Return a list of outlinks from a router */
    public int [] getOutLinks(int routerId) {
        return network_.getOutLinks(routerId);
    }

    /**
     * Is a router directly connected to another one
     */
    public boolean isConnected(int routerId, int other) {
        int [] links = getOutLinks(routerId);

        for (int p = 0; p < links.length; p++) {
            if (links[p] == other) {
                return true;
            }
        }

        return false;
    }

    /* Return a list of link costs from a router -- must be used in
     *  parallel get getOutLinks to id link nos*/
    public int [] getLinkCosts(int routerId) {
        return network_.getLinkCosts(routerId);
    }

    /** Create pair of integers  */
    static public Pair<Integer, Integer> makePair(int r1, int r2) {
        return new Pair<Integer, Integer>(r1, r2);
    }

    /** Remove a link with structures necessary in Global
     * Controller */
    public void unregisterLink(int router1Id, int router2Id) {
        network_.removeLink(router1Id, router2Id);
        APController_.removeLink(getElapsedTime(), router1Id, router2Id);

        // inform
        informAllRouters();
        informAllLinks();
    }

    /** Remove info about link from linkInfo struct */
    public void removeLinkInfo(Integer id) {
        linkInfo.remove(id);
    }

    /** Return the weight from link1 to link2 or 0 if no link*/
    public int getLinkWeight(int l1, int l2) {
        return network_.getLinkWeight(l1, l2);
    }


    /**
     * External and static access to start an Application
     */
    public  JSONObject appStart(int routerID, String className, String[] args) {
        try {
            AppStartEvent ev = new AppStartEvent(getElapsedTime(), null, routerID, className, args);
            JSONObject jsobj = executeEvent(ev);
            return jsobj;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * register an app
     */
    public void registerApp(int appID, int routerID) {
        appInfo.put(appID, routerID);
    }


    /**
     * Find some app info
     */
    public BasicRouterInfo findAppInfo(int appId) {
        int routerID = appInfo.get(appId);
        BasicRouterInfo bri = routerIdMap_.get(routerID);

        return bri;
    }


    /**
     * Find some app info, given an app ID
     * and returns a JSONObject.
     */
    public JSONObject findAppInfoAsJSON(int appID) throws JSONException {
        BasicRouterInfo bri = findAppInfo(appID);

        Logger.getLogger("log").logln(USR.STDOUT,"AppID: " + appID + " -> " + "BasicRouterInfo: " + bri);

        String appName = bri.getAppName(appID);

        Logger.getLogger("log").logln(USR.STDOUT,
            "AppID: " + appID + " -> " + "AppName: " + appName);

        Map<String, Object> data = bri.getApplicationData(appName);

        Logger.getLogger("log").logln(USR.STDOUT,
            "AppName: " + appName + " => " + "data: " + data);


        JSONObject jsobj = new JSONObject();
        jsobj.put("routerID", bri.getId());
        jsobj.put("appID", appID);
        jsobj.put("appName", appName);

        jsobj.put("args", data.get("Args"));
        jsobj.put("classname", data.get("ClassName"));
        jsobj.put("starttime", data.get("StartTime"));
        jsobj.put("runtime", data.get("RunTime"));

        /*
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            if (k.equals("classname") || k.equals("args") || k.equals("startime") ) {
                // store the value
                jsobj.put(k, v);
            }
        }
        */
        return jsobj;
    }


    /**
     * Is the app ID valid.
     */
    public boolean isValidAppID(int appId) {
        if (appInfo.get(appId) == null) {
            return false;
        } else {
            return true;
        }
    }


    // FIXME write this
    public boolean appStop(int appId) {
        Logger.getLogger("log").logln(USR.ERROR, "No way yet to stop applications");
        return false;
    }


    /** Request router stats */
    public void requestRouterStats() {
        try {
            // Get all LocalControllers
            for (LocalControllerInteractor lci : localControllers_) {
                lci.requestRouterStats();
            }
        } catch (IOException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        } catch (JSONException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        }
    }

    /**
     * Get the router stats -- method is blocking
     */
    public List<String> compileRouterStats() {
        try {
            List<String> result = new ArrayList<String>();

            // Get all LocalControllers
            for (LocalControllerInteractor lci : localControllers_) {
                List<String> routerStats = lci.getRouterStats();

                if (routerStats != null) {
                    result.addAll(routerStats);
                }
            }

            return result;
        } catch (IOException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return null;
        } catch (JSONException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return null;
        }
    }

    /*
     * Shutdown
     */
    public void shutDown() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "SHUTDOWN CALLED!");

        if (!options_.isSimulation()) {

            // stop all Routers
            for (int routerId : new ArrayList<Integer>(getRouterList())) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "SHUTDOWN router " + routerId);
                try {
                	EndRouterEvent re= new EndRouterEvent(getElapsedTime(),null,routerId);
                	// Execution is not through semaphore in shutDown since we have shutDown
                	re.execute(this);
                } catch (InstantiationException e) {
                	// TODO Auto-generated catch block
                	e.printStackTrace();
                }
            }


            // stop monitoring
            if (latticeMonitoring) {
                stopMonitoringConsumer();
                stopMonitoringProducer();
            }

            //ThreadTools.findAllThreads("GC pre killAllControllers:");
            killAllControllers();

            //ThreadTools.findAllThreads("GC post killAllControllers:");
            /*
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Pausing.");

            try {
                Thread.sleep(10);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
                System.exit(-1);
            }
            */

            //ThreadTools.findAllThreads("GC post checkMessages:");
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping console");

            stopConsole();

            //ThreadTools.findAllThreads("GC post stop console:");
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "All stopped, shut down now!");


    }

    /** Produce some output */
    public void produceOutput(long time, OutputType o) {
        File f;
        FileOutputStream s = null;
        PrintStream p = null;

        try {
            f = new File(o.getFileName());
            s = new FileOutputStream(f, true);
            p = new PrintStream(s, true);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot open " + o.getFileName()
                                          + " for output " + e.getMessage());
            return;
        }

        o.makeOutput(time, p, this);

        // Schedule next output time
        if (o.getTimeType() == OutputType.AT_INTERVAL) {
            OutputEvent e = new OutputEvent(time + o.getTime(), null, o);
            scheduler_.addEvent(e);
        }

        p.close();
        try {
            s.close();
        } catch (IOException ex) {
        }
    }

    /** Produce some output */
    public void produceEventOutput(Event ev, JSONObject response, OutputType o) {
        File f;
        FileOutputStream s = null;
        PrintStream p = null;

        try {
            f = new File(o.getFileName());
            s = new FileOutputStream(f, true);
            p = new PrintStream(s, true);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot open " + o.getFileName()
                                          + " for output " + e.getMessage());
            return;
        }

        o.makeEventOutput(ev, response, p, this);

        p.close();
        try {
            s.close();
        } catch (IOException ex) {
        }
    }

    public HashMap<String, int []> getTrafficLinkCounts() {
        return trafficLinkCounts_;
    }

    /** When output for traffic is requested then queue requests for traffic
     * from
     * routers */
    public void checkTrafficOutputRequests(long time, OutputType o) {
        if (options_.isSimulation()) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Request for output of traffic makes sense only"
                                          + " in context of emulation");
            return;
        }

        if (trafficOutputRequests_ == null) {
            trafficOutputRequests_ = new ArrayList<OutputType>();
            trafficOutputTime_ = new ArrayList<Long>();
        }

        trafficOutputRequests_.add(o);
        trafficOutputTime_.add(time);

        /** If requests already sent then just add it to the output
         * request
         * queue
         * rather than sending a further request */
        if (trafficOutputRequests_.size() > 1) {
            return;
        }
        //long start= getTime();
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Request for stats sent at time "+start);
        //  Make request for stats
        requestRouterStats();
       // Logger.getLogger("log").logln(USR.ERROR, leadin() + "Request for stats completed at time "+getTime()+ " elapsed (secs) "+((getTime() - start)/1000));
    }

    /** Receiver router traffic -- if it completes a set then output it */
    public void receiveRouterStats(String stats) {
        synchronized (routerStats_) {
            statsCount_++;

            routerStats_ = routerStats_.concat(stats);

            // System.err.println("Stat count is "+statsCount_);
            if (statsCount_ < localControllers_.size()) {
                // Not got all stats yet
                return;
            }

            //System.err.println("Enough"+routerStats_);
            File f;
            FileOutputStream s = null;
            PrintStream p = null;

            for (int i = 0; i < trafficOutputRequests_.size(); i++) {
                OutputType o = trafficOutputRequests_.get(i);
                try {
                    f = new File(o.getFileName());
                    s = new FileOutputStream(f, true);
                    p = new PrintStream(s, true);
                } catch (Exception e) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot open " + o.getFileName()
                                                  + " for output " + e.getMessage());
                    return;
                }

                OutputTraffic ot = (OutputTraffic)o.getOutputClass();
                ot.produceOutput(trafficOutputTime_.get(i), p, o, this);
            }

            //    System.err.println("Requests done");
            trafficOutputRequests_ = new ArrayList<OutputType>();
            trafficOutputTime_ = new ArrayList<Long>();
            statsCount_ = 0;

            NetStatsEvent nse = new NetStatsEvent(getElapsedTime(), routerStats_);
            addEvent(nse);
            routerStats_ = "";

            //    System.err.println("Finished here");
            p.close();
            try {
                s.close();
            } catch (IOException ex) {
            }
        }
    }

    /** Accessor function for routerStats_*/
    public String getRouterStats() {
        return routerStats_;
    }

    /**
     * Start listening for router stats using monitoring framework.
     */
    public synchronized void startMonitoringConsumer(InetSocketAddress addr) {
        // check to see if the monitoring is already connected and
        // running
        if (dataConsumer.isConnected()) {
            // if it is, stop it first
            stopMonitoringConsumer();
        }

        // set up DataPlane
        DataPlane inputDataPlane = new UDPDataPlaneForwardingConsumerWithNames(addr, forwardAddress);
        // WITH NO FORAWRDING. DataPlane inputDataPlane = new UDPDataPlaneConsumerWithNames(addr);

        dataConsumer.setDataPlane(inputDataPlane);



        // set the reporter
        dataConsumer.clearReporters();

        // add probes
        for (Reporter reporter : reporterMap.values()) {
            dataConsumer.addReporter(reporter);
        }

        // and connect
        boolean connected = dataConsumer.connect();

        if (!connected) {
            System.err.println("Cannot startMonitoringConsumer on " + addr + ". Address probably in use. Exiting.");
            System.exit(1);
        }
    }

    /**
     * Stop monitoring.
     */
    public synchronized void stopMonitoringConsumer() {
        if (dataConsumer.isConnected()) {
            dataConsumer.clearReporters(); // was setReporter(null);

            dataConsumer.disconnect();
        }
    }

    /**
     * Start producing router stats using monitoring framework.
     */
    public synchronized void startMonitoringProducer(InetSocketAddress addr) {
        // set up DataPlane
        DataPlane outputDataPlane = new UDPDataPlaneProducerWithNames(addr);
        dataSource.setDataPlane(outputDataPlane);

        // now setup a RouterLifecycleProbe to send out details
        // of the creation and shutdown of each Router

        Probe p = new RouterLifecycleProbe(this);
        probeList.add(p);

        // add probes
        for (Probe probe : probeList) {
            dataSource.addProbe(probe);  // this does registerProbe and activateProbe
        }

        // and connect
        boolean connected = dataSource.connect();

        if (!connected) {
            System.err.println("Cannot startMonitoringProducer on " + addr + ". Address probably in use. Exiting.");
            System.exit(1);
        }

        // turn on probes
        for (Probe probe : probeList) {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          leadin() + " data source: " +  dataSource.getName() + " turn on probe " +
                                          probe.getName());

            dataSource.turnOnProbe(probe);
        }

        System.out.println(leadin() + "Setup DataSource: " + dataSource);

    }

    /**
     * Stop monitoring Producer.
     */
    public synchronized void stopMonitoringProducer() {
        if (dataSource.isConnected()) {
            // turn off probes
            for (Probe probe : probeList) {
                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + " data source: " +  dataSource.getName() + " turn off probe " +
                                              probe.getName());

                dataSource.turnOffProbe(probe);

            }

            // disconnect
            dataSource.disconnect();

            // remove probes
            for (Probe probe : probeList) {
                dataSource.removeProbe(probe);
            }

        }

    }

    /**
     * Get the Reporter list of the monitoring data
     */
    public List<Reporter> getReporterList() {
        List<Reporter> list = new ArrayList<Reporter>();
        list.addAll(reporterMap.values());
        return list;
    }

    /**
     * Find reporter by label
     */
    public Reporter findByLabel(String label) {
        return reporterMap.get(label);
    }

    /**
     * Find reporter by Interface Class
     */
    public Reporter findByInterface(Class <?> inter) {
        // skip through each Reporter
        for (Reporter reporter :  reporterMap.values()) {
            // skip through each Interface
            for (Class<?> rI : reporter.getClass().getInterfaces()) {
                if (rI.isAssignableFrom(inter)) {
                    return reporter;
                }
            }
        }

        return null;
    }

    private void startLocalControllers() {
        Iterator<LocalControllerInfo> i = options_.getControllersIterator();
        Process child = null;

        while (i.hasNext()) {
            LocalControllerInfo lh = i.next();
            String [] cmd = options_.localControllerStartCommand(lh);
            try {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting process " + Arrays.asList(cmd));
                child = new ProcessBuilder(cmd).start();
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unable to execute remote command " + Arrays.asList(cmd));
                Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
                System.exit(-1);
            }

            String procName = lh.getName() + ":" + lh.getPort();
            childNames_.add(procName);
            childProcessWrappers_.put(procName, new ProcessWrapper(child, procName));

            try {
                Thread.sleep(100); // Simple wait is to ensure controllers start up
            } catch (java.lang.InterruptedException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "startLocalControllers Got interrupt!");
                System.exit(-1);
            }
        }
    }

    /**
     * An alive message has been received from the host specified
     * in LocalHostInfo.
     */
    public void aliveMessage(LocalHostInfo lh) {
        aliveCount += 1;
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Received alive count from " + lh.getName() + ":" + lh.getPort());
    }

    /**
     * Get the simulation start time.
     * This is the time the simulation actually started.
     */
    public long getStartTime() {
        if (options_.isSimulation()) {
            return 0;
        }

        return scheduler_.getStartTime();
    }

    /**
     * Get the time since the simulation started
     */
    public long getElapsedTime() {
    	if (isSimulation()) {
    		return scheduler_.getElapsedTime();
    	} else {
    		return System.currentTimeMillis() - scheduler_.getStartTime();
    	}
    }

    /** Gets the current time (From clock or the time of the last simulation event*/
    public long getTime() {
    	if (isSimulation()) {
    		return scheduler_.getElapsedTime();
    	} else {
    		return System.currentTimeMillis();
    	}
    }

    /**
     * Is the global controller running in simulation mode
     */

    public boolean isSimulation() {
        return options_.isSimulation();
    }

    /**
     * Convert an elasped time, in milliseconds, into a string.
     * Converts something like 35432 into 35:43
     */
    public String elapsedToString(long elapsedTime) {
        long millis = (elapsedTime % 1000) / 10;

        long rawSeconds = elapsedTime / 1000;
        long seconds = rawSeconds % 60;
        long minutes = rawSeconds / 60;

        StringBuilder builder = new StringBuilder();

        if (minutes < 10) {
            builder.append("0");
        }

        builder.append(minutes);

        builder.append(":");

        if (seconds < 10) {
            builder.append("0");
        }

        builder.append(seconds);

        builder.append(":");

        if (millis < 10) {
            builder.append("0");
        }

        builder.append(millis);
        return builder.toString();
    }

    /**
     * Get the ManagementConsole this ComponentController interacts with.
     */
    @Override
	public ManagementConsole getManagementConsole() {
        return console_;
    }

    /**
     * Get the APController
     */
    public APController getAPController() {
        return APController_;
    }

    /**
     * Check all controllers listed are functioning and
     * creates interactors with the LocalControllers.
     */
    private void checkAllControllers() {
        // try 20 times, with 500 millisecond gap
        int MAX_TRIES = 20;
        int tries = 0;
        int millis = 500;
        boolean isOK = false;

        localControllers_ = new ArrayList<LocalControllerInteractor>();
        interactorMap_
            = new HashMap<LocalControllerInfo,
                          LocalControllerInteractor>();
        LocalControllerInteractor inter = null;

        // lopp a bit and try and talk to the LocalControllers
        for (tries = 0; tries < MAX_TRIES; tries++) {
            // sleep a bit
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
            }

            // visit every LocalController
            for (int i = 0; i < noControllers_; i++) {
                LocalControllerInfo lh = options_.getController(i);

                if (interactorMap_.get(lh) == null) {
                    // we have not seen this LocalController before
                    // try and connect
                    try {
                        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Trying to make connection to "
                                                      + lh.getName() + " " + lh.getPort());
                        inter = new LocalControllerInteractor(lh);

                        localControllers_.add(inter);
                        interactorMap_.put(lh, inter);
                        inter.checkLocalController(myHostInfo_);

                        if (options_.getRouterOptionsString() != "") {
                            inter.setConfigString(options_.getRouterOptionsString());
                        }

                        // tell the LocalController to start monitoring
                        // TODO: make more robust
                        // only work if address is real
                        // and/ or there is a consumer
                        if (latticeMonitoring) {
                            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Setting  monitoring address: " + monitoringAddress + " timeout: " + monitoringTimeout);
                            inter.monitoringStart(monitoringAddress, monitoringTimeout);
                        }
                    } catch (Exception e) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Exception from " + lh + ". " + e.getMessage());
                        e.printStackTrace();
                        shutDown();
                        return;
                    }
                }
            }

            // check if we have connected to all of them
            // check if the no of controllers == the no of interactors
            // if so, we dont have to do all lopps
            if (noControllers_ == localControllers_.size()) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "All LocalControllers connected after " + (tries + 1) + " tries");
                isOK = true;
                break;
            }
        }

        // if we did all loops and it's not OK
        if (!isOK) {
            // couldnt reach all LocalControllers
            // We can keep a list of failures if we need to.
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Can't talk to all LocalControllers");
            shutDown();
            return;
        }

        // Wait to see if we have all controllers.
        for (int i = 0; i < options_.getControllerWaitTime(); i++) {
            if (aliveCount == noControllers_) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "All controllers responded with alive message.");
                return;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Only " + aliveCount
                                      + " from " + noControllers_ + " local Controllers responded.");
        shutDown();
        return;
    }

    /**
     * Send shutdown message to all controllers
     */
    private void killAllControllers() {
        if (localControllers_ == null) {
            return;
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping all controllers");
        LocalControllerInteractor inter;

        for (int i = 0; i < localControllers_.size(); i++) {
            inter = localControllers_.get(i);
            try {
                inter.shutDown();

                //ThreadTools.findAllThreads("GC post kill :" + i);
            } catch (IOException e) {
                System.err.println(
                    leadin()
                    + "Cannot send shut down to local Controller");
                System.err.println(e.getMessage());
            } catch (JSONException e) {
                System.err.println(
                    leadin()
                    + "Cannot send shut down to local Controller");
                System.err.println(e.getMessage());
            }
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stop messages sent to all controllers");

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping process wrappers");
        Collection<ProcessWrapper> pws = childProcessWrappers_.values();

        for (ProcessWrapper pw : pws) {
            pw.stop();
        }
    }

    /**
     * List all APs
     */
    public ArrayList<Integer> getAPs() {
        //return new ArrayList(new HashSet(apInfo.values()));
        return apList;
    }

    /**
     * Get the nominated AP for a router
     */
    public int getAP(int gid) {
        if (apInfo.containsKey(gid)) {
            return apInfo.get(gid);
        } else {
            return 0;
        }
    }

    public JSONObject setAP(long time,int gid, int AP) {
        return SetAggPointEvent.setAP(time,gid,AP,this);
    }

    public void registerAggPoint(long time,int gid, int AP) {
        apInfo.put(gid, AP);
        if (LifetimeEstimate.usingLifetimeEstimate()) {
        	LifetimeEstimate.getLifetimeEstimate().newAP(time, gid);
        }
        if (gid == AP) {
            apList.add(gid);
        } else {
            apList.remove(Integer.valueOf(gid));
        }

    }

    public void addEvent(Event e) {
        scheduler_.addEvent(e);
    }

    /** Router GID reports a connection to access point AP */
    public boolean reportAP(int gid, int AP) {
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "TODO write reportAP");
        return true;
    }

    public void addAPNode(long time, int rId) {
        APController_.addNode(time, rId);
    }

    public void addAPLink(long time, int router1Id, int router2Id) {
        APController_.addLink(time, router1Id, router2Id);
    }

    public boolean isLatticeMonitoring() {
        return latticeMonitoring;
    }

    public long getMaximumLag() {
        return options_.getMaxLag();
    }

    /** Accessor function for ControlOptions structure options_ */
    public ControlOptions getOptions() {
        return options_;
    }

    public int getMaxRouterId() {
        return network_.getLargestRouterId();
    }

    /** FIXME -- improve this */
    public int getNextNodeId() {
        return network_.getLargestRouterId();
    }

    public boolean connectedNetwork() {
        return options_.connectedNetwork();
    }

    public boolean allowIsolatedNodes() {
        return options_.allowIsolatedNodes();
    }

    public int getAPControllerConsiderTime() {
        return routerOptions_.getControllerConsiderTime();
    }

    /** Accessor function for routerList */
    public ArrayList<Integer> getRouterList() {
        return network_.getNodeList();
    }

    /** Return id of ith router */
    public int getRouterId(int i) {
        return network_.getNodeId(i);
    }

    /** Check if node is in network*/
    public boolean isRouterAlive(int i) {
        return network_.nodeExists(i);
    }

    /** Number of routers in simulation */
    public int getNoRouters() {
        return network_.getNoNodes();
    }

    /** Number of links in simulation */
    public int getNoLinks() {
        return network_.getNoLinks();
    }

    /** Access function for abstract network */
    public AbstractNetwork getAbstractNetwork() {
        return network_;
    }


    /**
     * Get the name of this GlobalController.
     */
    @Override
	public String getName() {
        if (myHostInfo_ == null) {
            return myName;
        }

        return myName + ":" + myHostInfo_.getPort();
    }

    /**
     * Set the startup file
     */
    public void setStartupFile(String file) {
        xmlFile_ = file;
    }

    String leadinFname() {
        return "GC";
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String GC = "GC: ";

        return getName() + " " + GC;
    }

}
