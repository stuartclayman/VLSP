package usr.globalcontroller;

import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.APcontroller.APController;
import usr.APcontroller.ConstructAPController;
import usr.common.ANSI;
import usr.common.BasicRouterInfo;
import usr.common.LinkInfo;
import usr.common.LocalHostInfo;
import usr.common.Pair;
import usr.common.PortPool;
import usr.common.ProcessWrapper;
import usr.console.ComponentController;
import usr.engine.APWarmUp;
import usr.engine.EventEngine;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventResolver;
import usr.events.EventScheduler;
import usr.events.ExecutableEvent;
import usr.events.SimpleEventScheduler;
import usr.events.globalcontroller.EndLinkEvent;
import usr.events.globalcontroller.EndRouterEvent;
import usr.events.globalcontroller.GCEventResolver;
import usr.events.globalcontroller.NetStatsEvent;
import usr.events.globalcontroller.OutputEvent;
import usr.events.globalcontroller.SetAggPointEvent;
import usr.events.globalcontroller.SetLinkWeightEvent;
import usr.events.globalcontroller.StartAppEvent;
import usr.events.globalcontroller.StartLinkEvent;
import usr.events.globalcontroller.StartRouterEvent;
import usr.events.globalcontroller.StopAppEvent;
import usr.interactor.LocalControllerInteractor;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import usr.model.abstractnetwork.AbstractLink;
import usr.model.abstractnetwork.AbstractNetwork;
import usr.model.lifeEstimate.LifetimeEstimate;
import usr.output.OutputTraffic;
import usr.output.OutputType;
import usr.router.RouterOptions;
import usr.vim.VimFunctions;
import cc.clayman.console.ManagementConsole;
import eu.reservoir.monitoring.appl.BasicConsumer;
import eu.reservoir.monitoring.appl.BasicDataSource;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.Reporter;
import eu.reservoir.monitoring.core.ReporterMeasurementType;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneForwardingConsumerWithNames;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneConsumerWithNames;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneProducerWithNames;

/**
 * The GlobalController is in overall control of the software.  It
 * contacts LocalControllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
public class GlobalController implements ComponentController, EventDelegate, VimFunctions {
    private ControlOptions options_;      // Options affecting the simulation

    // Options structure which is given to each router.
    private RouterOptions routerOptions_ = null;

    private String xmlFile_;              // name of XML file containing config
    private LocalHostInfo myHostInfo_;    // Information about the localhosts
    private GlobalControllerManagementConsole console_ = null;
    private ArrayList<LocalControllerInteractor> localControllers_ = null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;
    private ArrayList<String> childNames_ = null;

    // names of child processes

    private AbstractNetwork network_ = null;

    // Map connections LocalControllerInfo for given LCs to the appropriate interactors
    private HashMap<LocalControllerInfo, LocalControllerInteractor> interactorMap_ = null;

    // Map is used to store vacant ports on local controllers
    private HashMap<LocalControllerInfo, PortPool> portPools_ = null;

    // The PlacementEngine that determines where a Router is placed.
    private PlacementEngine placementEngine  = null;

    // Map is from router Id to information one which machine router is
    // stored on.
    private ConcurrentHashMap<Integer, BasicRouterInfo> routerIdMap_ = null;

    // A list of all the routers that have been shutdown
    private ArrayList<BasicRouterInfo> shutdownRouters_ = null;

    // A map of linkID to LinkInfo objects
    private HashMap<Integer, LinkInfo> linkInfo = null;

    // A Map if appID to routerID
    // i.e the router the app is running on
    private HashMap<Integer, Integer> appInfo = null;

    // A list of agg points
    private ArrayList<Integer> apList = null;
    // A map of routerID to the agg point for that router
    private HashMap<Integer, Integer> apInfo = null;

    private int aliveCount = 0;   // Counts number of live LocalControllers running.

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

    }

    /**
     * Construct a GlobalController -- this constructor contains things
     * which apply whether we are simulation or emulation
     */
    public GlobalController() {
    }

    /** Basic initialisation for the global controller */
    public void init() {
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

        // add some extra output channels, using mask bit 7, 8, 9, 10
        try {
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel7.out")), new BitMask(1<<7));
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel8.out")), new BitMask(1<<8));
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel9.out")), new BitMask(1<<9));
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel10.out")), new BitMask(1<<10));
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        // Redirect output for error and normal output if requested in
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
            } catch (FileNotFoundException e) {
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
            } catch (FileNotFoundException e) {
                System.err.println("Cannot output to file");
                System.err.println(fileName);
                System.exit(-1);
            }
        }

        // Set up AP controller
        APController_ = ConstructAPController.constructAPController(routerOptions_);

        try {
            myHostInfo_ = new LocalHostInfo(options_.getGlobalPort());
        } catch (UnknownHostException e) {
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


        // Setup Placement Engine
        String placementEngineClassName = options_.getPlacementEngineClassName();

        if (placementEngineClassName == null) {
            // there is no PlacementEngine defined in the options
            // use the built-in one
            placementEngineClassName = "usr.globalcontroller.LeastUsedLoadBalancer";
        }

        setupPlacementEngine(placementEngineClassName);  //new LeastBusyPlacement(this); // new LeastUsedLoadBalancer(this);

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

        // If any Engines need a Warm up - do it now
        if (options_.getWarmUpPeriod() > 0) {
            for (EventEngine e : options_.getEngines()) {
                if (e instanceof APWarmUp) {
                    ((APWarmUp)e).warmUp(scheduler_, options_.getWarmUpPeriod(), APController_, this);
                }
            }
        }

    }

    /**
     * Start the GlobalController.
     */
    public void start() {
        if (options_.isSimulation()) {
            runSimulation();
        } else {
            runEmulation();
        }
    }

    /**
     * Stop the GlobalController.
     */
    public void stop() {
        if (options_.isSimulation()) {
            endSimulation(System.currentTimeMillis());
        } else {
            shutDown();
        }
    }

    /**
     * Wrap and and clean up
     */
    private void wrapUp() {
        for (EventEngine e : options_.getEngines()) {
            e.finalEvents(this);
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

            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, ANSI.RED + leadin() + "Event exception " + e + ANSI.RESET_COLOUR);
                isActive_ = false;
            }
            /*
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
            */
        }

        shutDown();
    }

    /** Runs an emulation loop -- this spawns the scheduler as an independent
     *  process then waits.  The scheduler sends events back into the  main loop
     */

    private void runEmulation() {
        isActive_ = true;
        synchronized (runLoop_) {

            // Start Scheduler as thread
            scheduler_.start();


            while (isActive_) {
                try {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "runLoop_ wait");
                    runLoop_.wait();
                } catch (InterruptedException ie) {
                } catch (IllegalMonitorStateException ims) {
                }
            }
        }

        scheduler_.stop();

        shutDown();
    }

    /** 
     * Checks if the event controller is active 
     */
    public boolean isActive() {
        return isActive_;
    }

    /**
     * Called when an EventScheduler stops
     * Sets isActive_ to false ending the simulation */
    public void onEventSchedulerStop(long time) {
        isActive_ = false;

        if (!options_.isSimulation()) {
            synchronized (runLoop_) {
                runLoop_.notify();
            }
        }

        endSimulation(time);
    }

    /**
     * Called when an EventScheduler starts
     */
    public void onEventSchedulerStart(long time) {
        startSimulation(time);
    }

    /** 
     * Notification for an event execution success 
     */
    public void onEventSuccess(long time, Event ev) {
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Event "+ev+" success");
    }

    /**
     * Notification for an event execution failure
     */
    public void onEventFailure(long time, Event ev) {
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Event "+ev+" failed");
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

        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Checking existence of local Controllers");
        checkAllControllers();
    }

    /** Event for start Simulation */
    private void startSimulation(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Start of simulation  at: "
                                      + time + " " + System.currentTimeMillis());

        for (OutputType o : options_.getOutputs()) {
            if (o.getTimeType() == OutputType.AT_START) {
                produceOutput(time, o);
            }
        }
    }

    /** Event for end Simulation */
    private void endSimulation(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "End of simulation  at " + time
                                      + " " + System.currentTimeMillis());

        for (OutputType o : options_.getOutputs()) {
            if (o.getTimeType() == OutputType.AT_END) {
                produceOutput(time, o);
            }
        }

        wrapUp();
    }


    /**
     * Set up the PlacementEngine
     */
    private void setupPlacementEngine(String placementEngineClassName) {
        try {
            Class<?> c = Class.forName(placementEngineClassName);
            Class<? extends PlacementEngine> cc = c.asSubclass(PlacementEngine.class);

            // find Constructor for when arg is GlobalController
            Constructor<? extends PlacementEngine> cons = cc.getDeclaredConstructor(GlobalController.class);

            placementEngine = cons.newInstance(this);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Setup PlacementEngine: " + placementEngine);

        } catch (ClassNotFoundException cnfe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Class not found " + placementEngineClassName);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot instantiate class " + placementEngineClassName);
        }
    }

    /**
     * Set up the reporters
     */
    private void setupReporters(HashMap<String, String> reporterInfoMap) {
        // skip through the map, instantiate a Probe and set its data
        // rate
        for (Map.Entry<String, String> entry : reporterInfoMap.entrySet()) {
            String reporterClassName = entry.getValue();
            String label = entry.getKey();

            try {
                // Now convert the class name to a Class
                // get Class object
                // WAS Class<Reporter> cc =
                // (Class<Reporter>)Class.forName(reporterClassName);

                // Replaced with following 2 lines
                Class<?> c = Class.forName(reporterClassName);
                Class<? extends Reporter> cc = c.asSubclass(Reporter.class);

                // find Constructor for when arg is GlobalController
                Constructor<? extends Reporter> cons = cc.getDeclaredConstructor(GlobalController.class);

                Reporter reporter = cons.newInstance(this);
                reporterMap.put(label, reporter);

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Added reporter: " + label + " -> " + reporter);
            } catch (ClassNotFoundException cnfe) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Class not found " + reporterClassName);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot instantiate class " + reporterClassName);
            }
        }
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
    private JSONObject semaphoredOperation(Operation op) throws Exception, TimeoutException, InterruptedException {
        try {
            long t0 = System.currentTimeMillis();
            
            // Wait to aquire a lock -- only one event at once
            //
            boolean acquired = semaphore.tryAcquire(options_.getMaxLag(), TimeUnit.MILLISECONDS);
            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + ANSI.YELLOW + "GOT SEMAPHORE" + ANSI.RESET_COLOUR);

            if (!acquired) {
                throw new TimeoutException(leadin()+"GlobalController lagging too much");
            }

            if (!isActive_) {
                throw new InterruptedException("Run finished!");
            }

            JSONObject js = null;

            js = op.call();

            long t1 = System.currentTimeMillis();

            // time in millis
            long opTime = t1 - t0;
            js.put("op_time", opTime);
            
            return js;
        } finally {
            semaphore.release();
            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + ANSI.YELLOW + "RELEASE SEMAPHORE" + ANSI.RESET_COLOUR);
        }
    }

    /** Execute an event,
     * return a JSON object with information about it
     * throws Instantiation if creation fails
     * Interrupted if acquisition of lock interrupted
     * Timeout if acquisition timesout
     */
    @Override
    public JSONObject executeEvent(Event ev) {

        try {
            // Get a 'final' handle on the Event
            final Event e = ev;
            // Get a 'final' handle on the GlobalController
            final GlobalController gc = this;

            // Define the Operation body
            // the method 'call()' is called by semaphoredOperation()
            Operation execute = new Operation() {
                    @Override
                    public JSONObject call() throws InstantiationException {
                        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"EVENT preceed: " + e);

                        ExecutableEvent ee = null;

                        if (e instanceof ExecutableEvent) {
                            ee = (ExecutableEvent) e;
                        } else {
                            // resolve event
                            EventResolver resolver = new GCEventResolver();

                            ee = resolver.resolveEvent(e);

                            if (ee == null) {
                                Logger.getLogger("log").logln(USR.ERROR, ANSI.RED + "EVENT not ExecutableEvent: " + e + ANSI.RESET_COLOUR);
                                return null;
                            }

                            // tell the event which EventScheduler we are using
                            ee.setEventScheduler(scheduler_);
                        }


                        // event preceeed
                        ee.preceedEvent(gc);


                        // pass in gc as EventDelegate and as context object
                        JSONObject js = ee.execute(gc, gc);
                        //Logger.getLogger("log").logln(USR.STDOUT, "EVENT result:  " + js);


                        // event follow
                        if (js != null) {
                            try {
                                if (js.getBoolean("success")) {
                                    ee.followEvent(js, gc);
                                }
                            } catch (JSONException je) { }
                        }



                        for (OutputType t : eventOutput_) {
                            produceEventOutput(e, js, t);
                        }

                        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"EVENT done: " + e );
                        String str = js.toString();

                        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+ " "+str.substring(0, Math.min(str.length(), 60)));

                        return js;
                    }

                };

            // Do the Operation in the semaphore code block
            JSONObject jsobj = semaphoredOperation(execute);

            return jsobj;
        } catch (TimeoutException te) {
            //te.printStackTrace();

            JSONObject jsobj = new JSONObject();

            try {
                jsobj.put("success", false);
                jsobj.put("msg", te.getMessage());
            } catch (JSONException je) {
            }

            return jsobj;

        } catch (InterruptedException ie) {
            // ie.printStackTrace();

            JSONObject jsobj = new JSONObject();
            try {
                jsobj.put("success", false);
                jsobj.put("msg", ie.getMessage());
            } catch (JSONException je) {
            }

            return jsobj;

        } catch (Exception e) {
            e.printStackTrace();

            JSONObject jsobj = new JSONObject();

            try {
                jsobj.put("success", false);
                jsobj.put("msg", e.getMessage());
            } catch (JSONException je) {
            }

            return jsobj;

        }
    }

    /** Convenience function to create JSON object from error string*/
    public JSONObject commandError(String error) {
        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("msg", "ERROR: " + error);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "JSON creation error in commandError");
        }

        return jsobj;
    }


    /**
     * Get the LocalControllers which are online.
     */
    public Set<LocalControllerInfo> getLocalControllers() {
        Set<LocalControllerInfo> lcSet = interactorMap_.keySet();
        Set<LocalControllerInfo> result = new HashSet<LocalControllerInfo>();

        for (LocalControllerInfo lc : lcSet) {
            if (lc.getActiveStatus() == LocalControllerInfo.LocalControllerActiveStatus.ONLINE) {
                result.add(lc);
            }
        }

        return result;
    }

    /**
     * Get all the LocalControllers
     */
    public Set<LocalControllerInfo> getAllLocalControllers() {
        return interactorMap_.keySet();
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

    /**
     * Turn on an offline LocalController.
     * If the LocalController is OFFLINE and the operation is
     * successful, then return true.
     * If the LocalController is already ONLINE, return false.
     */
    public boolean takeLocalControllerOnline(String value) {
        if (isValidLocalControllerID(value)) {

            LocalControllerInfo lci = findLocalControllerInfo(value);

            if (lci.getActiveStatus() == LocalControllerInfo.LocalControllerActiveStatus.OFFLINE) {
                lci.setActiveStatus(LocalControllerInfo.LocalControllerActiveStatus.ONLINE);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Turn on an online LocalController.
     * If the LocalController is ONLINE and the operation is
     * successful, then return true.
     * If the LocalController is already OFFLINE, return false.
     * Note an ONLINE LocalController can only be made OFFLINE
     * if it has Zero managed routers.
     */
    public boolean takeLocalControllerOffline(String value) {
        if (isValidLocalControllerID(value)) {

            LocalControllerInfo lci = findLocalControllerInfo(value);

            if (lci.getActiveStatus() == LocalControllerInfo.LocalControllerActiveStatus.ONLINE) {
                // now check no of routers
                if (lci.getNoRouters() == 0) {
                    lci.setActiveStatus(LocalControllerInfo.LocalControllerActiveStatus.OFFLINE);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /** Get the port pool associated with a local controller */
    public PortPool getPortPool(LocalControllerInfo lci) {
        return portPools_.get(lci);
    }

    /**
     * Get a mapping of host to the list of routers on that host.
     */
    public HashMap<LocalControllerInfo, List<BasicRouterInfo> > getRouterLocations() {
        HashMap<LocalControllerInfo, List<BasicRouterInfo> > routerLocations = new HashMap<LocalControllerInfo, List<BasicRouterInfo> >();

        // work out which router is where
        for (BasicRouterInfo routerInfo : getAllRouterInfo()) {
            LocalControllerInfo localInfo = routerInfo.getLocalControllerInfo();

            if (routerLocations.containsKey(localInfo)) { // we've seen this host
                List<BasicRouterInfo> list = routerLocations.get(localInfo);
                list.add(routerInfo);
            } else {                                 //  it's a new host
                List<BasicRouterInfo> list = new ArrayList<BasicRouterInfo>();
                list.add(routerInfo);

                routerLocations.put(localInfo, list);
            }
        }

        return routerLocations;
    }

    /**
     * Do some placement calculation
     */
    public LocalControllerInfo placementForRouter(String name, String address) {
        return placementEngine.routerPlacement(name, address);
    }


    /** Register existence of router */
    public void registerRouter(long time,int rId) {
        network_.addNode(rId);

        // Tell APController about link
        APController_.addNode(time, rId);

        // inform about all routers
        informAllRouters();

        Logger.getLogger("log").logln(1<<9, elapsedToString(getElapsedTime()) + ANSI.GREEN + " START ROUTER " + rId + ANSI.RESET_COLOUR);

    }

    /** Unregister a router and all links from structures in
     *  GlobalController*/
    public void unregisterRouter(long time, int rId) {
        int[] out = network_.getOutLinks(rId);
        APController_.removeNode(time, rId);

        for (int i = out.length - 1; i >= 0; i--) {
            unregisterLink(time, rId, out[i]);
        }

        network_.removeNode(rId);

        // inform about all routers
        informAllRouters();

        Logger.getLogger("log").logln(1<<9, elapsedToString(getElapsedTime()) + ANSI.RED + " STOP ROUTER " + rId + ANSI.RESET_COLOUR);



    }

    /** Find some router info
     */
    public BasicRouterInfo findRouterInfo(int rId) {
        return routerIdMap_.get(rId);
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
     * Find some local controller info, given a local controller ip address or a local controller name
     */
    public LocalControllerInfo findLocalControllerInfo(String value) {
        // skip through all the LocalControllerInfo objects
        for (LocalControllerInfo info : getAllLocalControllers()) {
            if (value.contains(":")) {
                // a name and port has been passed in
                String[] parts = value.split(":");
                String name = parts[0];
                int port = 0;

                // try port
                Scanner scanner = new Scanner(parts[1]);

                if (scanner.hasNextInt()) {
                    port = scanner.nextInt();
                    scanner.close();
                } else {
                    scanner.close();
                }

                if (info.getName() != null && info.getName().equals(name) &&
                    info.getPort() == port) {
                    // we found a match
                    return info;
                }

            } else {
                if (info.getIp() != null && info.getIp().equals(value)) {
                    // we found a match
                    return info;
                } else if (info.getName() != null && info.getName().equals(value)) {
                    // we found a match
                    return info;
                }
            }
        }
        // we got here and found nothing
        return null;
    }


    /**
     * Is a LocalController name valid
     */
    public boolean isValidLocalControllerID(String value) {
        LocalControllerInfo info = findLocalControllerInfo(value);

        if (info == null) {
            return false;
        } else {
            return true;
        }
    }
    /**
     * List all RouterInfo.
     */
    public Collection<BasicRouterInfo> getAllRouterInfo() {
        return routerIdMap_.values();
    }

    /**
     * Find some router info, given a router ID
     * and return a JSONObject
     */
    private JSONObject findRouterInfoAsJSON(int routerID) throws JSONException {
        BasicRouterInfo bri = findRouterInfo(routerID);

        return routerInfoAsJSON(bri);
    }


    /**
     * Find some router info, given a router address or a router name
     * and return a JSONObject
     */
    private JSONObject findRouterInfoAsJSON(String value) throws JSONException {
        BasicRouterInfo bri = findRouterInfo(value);

        return routerInfoAsJSON(bri);
    }


    /**
     * Convert BasicRouterInfo into JSON
     */
    private JSONObject routerInfoAsJSON(BasicRouterInfo bri) throws JSONException {
        int routerID = bri.getId();

        JSONObject jsobj = new JSONObject();

        jsobj.put("time", bri.getTime());
        jsobj.put("routerID", bri.getId());
        jsobj.put("name", bri.getName());
        jsobj.put("address", bri.getAddress());
        jsobj.put("mgmtPort", bri.getManagementPort());
        jsobj.put("r2rPort", bri.getRoutingPort());

        // now get all outlinks
        Collection<LinkInfo> links = findLinkInfoByRouter(routerID);

        JSONArray outArr = new JSONArray();
        JSONArray linkIDArr = new JSONArray();

        for (LinkInfo li : links) {
            int otherEnd = li.getEndPoints().getSecond() == routerID ? li.getEndPoints().getFirst() : li.getEndPoints().getSecond();
            outArr.put(otherEnd);
            linkIDArr.put(li.getLinkID());
        }

        //int [] outLinks = getOutLinks(routerID);

        //for (int outLink : outLinks) {
        //    outArr.put(outLink);
        //}

        jsobj.put("links", outArr);
        jsobj.put("linkIDs", linkIDArr);

        return jsobj;

    }

    /**
     * Convert LocalControllerInfo into JSON
     */
    private JSONObject localControllerInfoAsJSON(LocalControllerInfo lci) throws JSONException {

        JSONObject jsobj = new JSONObject();

        String localControllerName=lci.getName() + ":" + lci.getPort();

        jsobj.put("name", localControllerName);
        jsobj.put("IP", lci.getIp());
        jsobj.put("port", lci.getPort());
        jsobj.put("maxRouters", lci.getMaxRouters());
        jsobj.put("noRouters", lci.getNoRouters());
        jsobj.put("usage", lci.getUsage());

        // show router IDs
        JSONArray array = new JSONArray();  // ID array
        for (int routerID : lci.getRouters()) {
            array.put(routerID);
        }
        jsobj.put("routers", array);


        // get status
        jsobj.put("status", lci.getActiveStatus());


        if (lci.getRemoteLoginUser() != null) {
            jsobj.put("remoteLoginUser", lci.getRemoteLoginUser());
        }

        if (lci.getRemoteStartController() != null) {
            jsobj.put("remoteStartController", lci.getRemoteStartController());
        }

        // Energy Factors
        JSONObject energyFactors = new JSONObject();
        energyFactors.put("cpuIdleCoefficient", lci.GetCPUIdleCoefficient());
        energyFactors.put("cpuLoadCoefficient", lci.GetCPULoadCoefficient());
        energyFactors.put("freeMemoryCoefficient", lci.GetFreeMemoryCoefficient());
        energyFactors.put("memoryAllocationCoefficient", lci.GetMemoryAllocationCoefficient());
        energyFactors.put("networkIncomingBytesCoefficient", lci.GetNetworkIncomingBytesCoefficient());
        energyFactors.put("networkOutboundBytesCoefficient", lci.GetNetworkOutboundBytesCoefficient());
        energyFactors.put("baseLineEnergyConsumption", lci.GetBaseLineEnergyConsumption());

                
        jsobj.put("energyFactors", energyFactors);

        // retrieve current status information
        HostInfoReporter hostInfoReporter = (HostInfoReporter) findByMeasurementType("HostInfo");
        JSONObject measurementJsobj = null;

        measurementJsobj = hostInfoReporter.getProcessedData(localControllerName); 


        /*
        // current status of localcontroller
        float currentCPUUserAndSystem=0;
        float currentCPUIdle=0;
        float currentMemoryUsed=0;
        float currentFreeMemory=0;
        long currentOutputBytes=0;
        long currentInputBytes=0;

        if (measurementJsobj!=null) {
            // extracted required measurements for the energy model
            try {
                currentCPUUserAndSystem = (float) measurementJsobj.getDouble("cpuLoad");
                currentCPUIdle = (float) measurementJsobj.getDouble("cpuIdle");
                currentMemoryUsed = measurementJsobj.getInt("usedMemory");
                currentFreeMemory = measurementJsobj.getInt("freeMemory");
                currentOutputBytes = measurementJsobj.getLong("networkOutboundBytes");
                currentInputBytes = measurementJsobj.getLong("networkIncomingBytes");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            // Get energy usage
            double energyConsumption = lci.GetCurrentEnergyConsumption(currentCPUUserAndSystem, currentCPUIdle, currentMemoryUsed, currentFreeMemory, currentOutputBytes, currentInputBytes);

            jsobj.put("energyConsumption", energyConsumption);
        }
        */

        jsobj.put("hostinfo", measurementJsobj);



        



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
    protected void informRouterEnded(String name) {
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
     * List all RouterInfo as a JSON object
     */
    private JSONObject getAllRouterInfoAsJSON(String detail) throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();  // ID array
        JSONArray detailArray = new JSONArray();  // detail array


        ThreadGroupListReporter threadGroupListReporter = (ThreadGroupListReporter) findByMeasurementType("ThreadGroupList");


        for (BasicRouterInfo info : getAllRouterInfo()) {
            int routerID = info.getId();

            String routerName = info.getName();

            array.put(routerID);

            if (detail.equals("all")) {
                // add a detailed record
                JSONObject record = routerInfoAsJSON(info);

                detailArray.put(record);

            } else if (detail.equals("thread")) {
                // add a detailed record
                JSONObject record = routerInfoAsJSON(info);

                detailArray.put(record);

                

            } else if (detail.equals("threadgroup")) {
                // add a detailed record
                JSONObject record = routerInfoAsJSON(info);
                
                // get threadgroupObj
                JSONObject threadgroupObj = threadGroupListReporter.getProcessedData(routerName);

                record.put("threadgroup", threadgroupObj.getJSONArray("threadgroup"));

                detailArray.put(record);
            }
        }

        jsobj.put("type", "router");
        jsobj.put("list", array);

        if (detail.equals("all") || detail.equals("thread") || detail.equals("threadgroup") ) {
            jsobj.put("detail", detailArray);
        }

        return jsobj;
    }

    /**
     * List all LocalControllerInfo as a JSON object
     */
    private JSONObject getAllLocalControllerInfoAsJSON(String detail) throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray detailArray = new JSONArray();

        for (LocalControllerInfo info : getAllLocalControllers()) {
            String localControllerName = info.getName() + ":" + info.getPort();

            array.put(localControllerName);

            if (detail.equals("all")) {
                // add a detailed record
                JSONObject record = localControllerInfoAsJSON(info);

                detailArray.put(record);

            }
        }

        jsobj.put("type", "localcontroller");
        jsobj.put("list", array);

        if (detail.equals("all")) {
            jsobj.put("detail", detailArray);
        }

        return jsobj;
    }

    /**
     * List one RouterInfo as a JSON object
     */
    private JSONObject getOneRouterInfoAsJSON(String value) throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray detailArray = new JSONArray();


        BasicRouterInfo bri = findRouterInfo(value);
        int routerID = bri.getId();

        array.put(routerID);

        JSONObject record = routerInfoAsJSON(bri);

        detailArray.put(record);

        jsobj.put("type", "router");
        jsobj.put("list", array);

        jsobj.put("detail", detailArray);

        return jsobj;
    }

    /**
     * List one LocalControllerInfo as a JSON object
     */
    private JSONObject getOneLocalControllerInfoAsJSON(String value) throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray detailArray = new JSONArray();

        LocalControllerInfo lci = findLocalControllerInfo(value);

        if (lci != null) {
            String localControllerName = lci.getName() + ":" + lci.getPort();

            array.put(localControllerName);

            JSONObject record = localControllerInfoAsJSON(lci);

            detailArray.put(record);

            jsobj.put("type", "localcontroller");
            jsobj.put("list", array);

            jsobj.put("detail", detailArray);

            return jsobj;
        } else {
            
            try {
                jsobj.put("msg", "ERROR: " + "no localcontroller at " + value);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, "JSON creation error in getOneLocalControllerInfoAsJSON");
            }

            return jsobj;

        }
    }


    
    /**
     * Make LocalController Online, return data as a JSON object
     */
    public JSONObject takeLocalControllerOnlineJSON(String value) throws JSONException {
        JSONObject jsobj = new JSONObject();

        LocalControllerInfo lci = findLocalControllerInfo(value);
        String localControllerName = lci.getName() + ":" + lci.getPort();

        if (lci != null) {
            boolean opResult = takeLocalControllerOnline(value);

            jsobj.put("name", localControllerName);
            jsobj.put("type", "localcontroller");
            jsobj.put("status", lci.getActiveStatus());
            jsobj.put("success", opResult);

            if (opResult == false) {
                jsobj.put("msg", "LocalController " + localControllerName + " already ONLINE");
            }
            
            return jsobj;

        } else {
            
            try {
                jsobj.put("msg", "ERROR: " + "no localcontroller at " + value);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, "JSON creation error in getOneLocalControllerInfoAsJSON");
            }

            return jsobj;

        }
    }


    /**
     * Make LocalController Offline, return data as a JSON object
     */
    public JSONObject takeLocalControllerOfflineJSON(String value) throws JSONException {
        JSONObject jsobj = new JSONObject();

        // first we check if this is the last online LocalController
        // if it is, we cannot take it offline
        int locCount = getLocalControllers().size();

        if (locCount == 1) {
            // this is the last one
            // failed to make offline
            jsobj.put("name", value);
            jsobj.put("type", "localcontroller");
            jsobj.put("status", LocalControllerInfo.LocalControllerActiveStatus.ONLINE);
            jsobj.put("success", false);
                
            jsobj.put("msg", "LocalController cannot be shutdown. Last ONLINE localController");

            return jsobj;

        } else {
            // try and take the LocalController offline
            LocalControllerInfo lci = findLocalControllerInfo(value);
            String localControllerName = lci.getName() + ":" + lci.getPort();

            if (lci != null) {
                boolean opResult = takeLocalControllerOffline(value);

                if (opResult) { // successfully made offline

                    jsobj.put("name", localControllerName);
                    jsobj.put("type", "localcontroller");
                    jsobj.put("status", lci.getActiveStatus());
                    jsobj.put("success", true);

                    return jsobj;
                } else {
                    // failed to make offline
                    jsobj.put("name", localControllerName);
                    jsobj.put("type", "localcontroller");
                    jsobj.put("status", lci.getActiveStatus());
                    jsobj.put("success", false);
                
                    if (lci.getNoRouters() == 0) {
                        jsobj.put("msg", "LocalController " + localControllerName + " already OFFLINE");
                    } else {
                        jsobj.put("noRouters", lci.getNoRouters());
                        jsobj.put("msg", "LocalController " + localControllerName + " has active routers");
                    }
                }

                return jsobj;

            } else {
            
                try {
                    jsobj.put("msg", "ERROR: " + "no localcontroller at " + value);
                } catch (Exception e) {
                    Logger.getLogger("log").logln(USR.ERROR, "JSON creation error in getOneLocalControllerInfoAsJSON");
                }

                return jsobj;

            }
        }
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
    public List<BasicRouterInfo> getShutdownRouters() {
        return shutdownRouters_;
    }

    /**
     * List all shutdown routers
     */
    private JSONObject listShutdownRoutersAsJSON() throws JSONException {
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
    private JSONObject findLinkInfoAsJSON(int linkID) throws JSONException {
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
    private JSONObject getAllLinkInfoAsJSON(String detail) throws JSONException {
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
     * Get a list of Router Link Info as JSON.
     * Get info on all links from a specified router.
     */
    private JSONObject listRouterLinksAsJSON(int routerID, String attr) throws JSONException {
        Collection<LinkInfo> links = findLinkInfoByRouter(routerID);

        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();

        for (LinkInfo link : links) {

            if (attr.equals("id")) {
                array.put(link.getLinkID());
            } else if (attr.equals("name")) {
                array.put(link.getLinkName());
            } else if (attr.equals("weight")) {
                array.put(link.getLinkWeight());
            } else if (attr.equals("connected")) {
                Pair<Integer, Integer> routers = link.getEndPoints();

                // put out router ID of other end
                if (routers.getSecond() == routerID) {
                    array.put(routers.getFirst());
                } else {
                    array.put(routers.getSecond());
                }
            } else {
                // should not get here
                throw new Error("RouterLinkRestHandler: should not get here");
            }
        }

        jsobj.put("routerID", routerID);
        jsobj.put("type", "link");
        jsobj.put("list", array);

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
    private JSONObject getRouterLinkStatsAsJSON(int routerID) throws JSONException {
        // Find the traffic reporter
        // This is done by asking the GlobalController for
        // a class that implements TrafficInfo.
        // It is this class that has the current traffic info.
        Reporter netIFStatsReporter = findByMeasurementType("NetIFStats");

        // We know it implements TrafficInfo
        TrafficInfo reporter = (TrafficInfo)netIFStatsReporter;

        //TrafficInfo reporter = (TrafficInfo)findByInterface(TrafficInfo.class);


        findLinkInfoByRouter(routerID);

        // result object
        JSONObject jsobj = new JSONObject();

        // array for links
        JSONArray linkArr = new JSONArray();
        // array for stats
        JSONArray statArr = new JSONArray();


        // get outlinks
        int [] outLinks = network_.getOutLinks(routerID);

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
    private JSONObject getRouterLinkStatsAsJSON(int routerID, int dstID) throws JSONException {
        // Find the traffic reporter
        // This is done by asking the GlobalController for
        // a class that implements TrafficInfo.
        // It is this class that has the current traffic info.
        Reporter netIFStatsReporter = findByMeasurementType("NetIFStats");

        // We know it implements TrafficInfo
        TrafficInfo reporter = (TrafficInfo)netIFStatsReporter;

        //TrafficInfo reporter = (TrafficInfo)findByInterface(TrafficInfo.class);


        findLinkInfoByRouter(routerID);

        // result object
        JSONObject jsobj = new JSONObject();

        // array for links
        JSONArray linkArr = new JSONArray();
        // array for stats
        JSONArray statArr = new JSONArray();


        // get outlinks
        int [] outLinks = network_.getOutLinks(routerID);

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
    public void registerLink(long time, int router1Id, int router2Id, boolean scheduled) {
        network_.addLink(router1Id, router2Id, scheduled);

        // Tell APController about link
        APController_.addLink(time, router1Id, router2Id);

        // inform
        informAllRouters();
        informAllLinks();

        Logger.getLogger("log").logln(1<<9, elapsedToString(getElapsedTime()) + ANSI.BLUE + " CREATE LINK " + router1Id + " TO " + router2Id + ANSI.RESET_COLOUR);


    }

    /** Remove a link with structures necessary in Global Controller */
    public void unregisterLink(long time, int router1Id, int router2Id) {
        network_.removeLink(router1Id, router2Id);
        APController_.removeLink(time, router1Id, router2Id);

        // inform
        informAllRouters();
        informAllLinks();

        Logger.getLogger("log").logln(1<<9, elapsedToString(getElapsedTime()) + ANSI.MAGENTA + " REMOVE LINK " + router1Id + " TO " + router2Id + ANSI.RESET_COLOUR);

    }

    /* Return a list of outlinks from a router */
    //public List<Integer> getOutLinksList(int routerId) {
    //  return (List<Integer>)Arrays.asList(network_.getOutLinks(routerId));
    //}

    /**
     * Is a router directly connected to another one
     */
    public boolean isConnected(int routerId, int other) {
        int [] links = network_.getOutLinks(routerId);

        for (int p = 0; p < links.length; p++) {
            if (links[p] == other) {
                return true;
            }
        }

        return false;
    }

    /** Create pair of integers  */
    public Pair<Integer, Integer> makePair(int r1, int r2) {
        return new Pair<Integer, Integer>(r1, r2);
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
     * Schedule the creation of a link between two nodes
     */
    public void scheduleLink(AbstractLink link, EventEngine eng, long time) {
        StartLinkEvent sle = new StartLinkEvent(time, eng, link);
        sle.setScheduled();
        scheduler_.addEvent(sle);
        network_.scheduleLink(link);
    }

    // /**
    //  * Schedule the creation of a link between two nodes
    //  */
    // public void scheduleLink(int node1, int node2, EventEngine eng, long time) {
    //     AbstractLink l = new AbstractLink(node1, node2);
    //     scheduleLink(l, eng, time);
    // }

    /**
     * register an app
     */
    public void registerApp(long time, int appID, int routerID) {
        appInfo.put(appID, routerID);
    }

    /**
     * unregister an app
     */
    public void unregisterApp(long time, int appID) {
        appInfo.remove(appID);
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
     * 
     */
    private JSONObject getAllAppInfoAsJSON(int routerID) throws JSONException {
        BasicRouterInfo bri = findRouterInfo(routerID);

        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();

        for (Integer id : bri.getApplicationIDs()) {

            array.put(id);
        }

        jsobj.put("type", "app");
        jsobj.put("list", array);
        jsobj.put("routerID", routerID);

        return jsobj;
    }

    /**
     * Find some app info, given an app ID
     * and returns a JSONObject.
     */
    private JSONObject findAppInfoAsJSON(int appID) throws JSONException {
        BasicRouterInfo bri = findAppInfo(appID);

        Logger.getLogger("log").logln(USR.STDOUT, "AppID: " + appID + " -> " + "BasicRouterInfo: " + bri);

        String appName = bri.getAppName(appID);

        Logger.getLogger("log").logln(USR.STDOUT, "AppID: " + appID + " -> " + "AppName: " + appName);

        Map<String, Object> data = bri.getApplicationData(appName);

        Logger.getLogger("log").logln(USR.STDOUT, "AppName: " + appName + " => " + "data: " + data);


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
        throw new Error("REMOVED 20140104 - sclayman");
    }
    /*
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
    */

    /*
     * Shutdown
     */
    public void shutDown() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "SHUTDOWN CALLED!");

        wrapUp();

        if (!options_.isSimulation()) {

            // stop all Routers
            for (int routerId : new ArrayList<Integer>(getRouterList())) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "SHUTDOWN router " + routerId);

                // Execution is not through semaphore in shutDown since we have shutDown
                EndRouterEvent re = new EndRouterEvent(getElapsedTime(), null, routerId);
                re.execute(this);
            }

            // stop monitoring
            if (latticeMonitoring) {
                stopMonitoringConsumer();
                stopMonitoringProducer();
            }

            //ThreadTools.findAllThreads("GC pre killAllControllers:");
            killAllControllers();

            //ThreadTools.findAllThreads("GC post killAllControllers:");

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
        } catch (FileNotFoundException e) {
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
    private void produceEventOutput(Event ev, JSONObject response, OutputType o) {
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

        long start= getTime();
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Request for stats sent at time "+start);
        //  Make request for stats
        requestRouterStats();

        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Request for stats completed at time "+getTime()+ " elapsed (secs) "+((getTime() - start)/1000));
    }

    /** Receiver router traffic -- if it completes a set then output it */
    public void receiveRouterStats(String stats) {
        synchronized (routerStats_) {
            // System.err.println("receiveRouterStats: top");
            statsCount_++;

            routerStats_ = routerStats_.concat(stats);

            // System.err.println("Stat count is "+statsCount_);
            if (statsCount_ < localControllers_.size()) {
                // Not got all stats yet
                return;
            }

            // System.err.println("receiveRouterStats: Enough"+routerStats_);
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

            // System.err.println("receiveRouterStats: Requests done");
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
    private synchronized void startMonitoringConsumer(InetSocketAddress addr) {
        // check to see if the monitoring is already connected and
        // running
        if (dataConsumer.isConnected()) {
            // if it is, stop it first
            stopMonitoringConsumer();
        }

        // set up DataPlane
        // WITH FORWARDING
        // DataPlane inputDataPlane = new UDPDataPlaneForwardingConsumerWithNames(addr, forwardAddress);
        // WITH NO FORAWRDING
        DataPlane inputDataPlane = new UDPDataPlaneConsumerWithNames(addr);

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
    private synchronized void stopMonitoringConsumer() {
        if (dataConsumer.isConnected()) {
            dataConsumer.clearReporters(); // was setReporter(null);

            dataConsumer.disconnect();
        }
    }

    /**
     * Start producing router stats using monitoring framework.
     */
    private synchronized void startMonitoringProducer(InetSocketAddress addr) {
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
    private synchronized void stopMonitoringProducer() {
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
    public Reporter findByInterface(Class<?> inter) {
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

    /**
     * Find reporter by Measurement type it consumes and reports on
     */
    public Reporter findByMeasurementType(String type) {
        // skip through each Reporter
        for (Reporter reporter :  reporterMap.values()) {
            if (reporter instanceof ReporterMeasurementType) {
                List<String> types = ((ReporterMeasurementType)reporter).getMeasurementTypes();

                if (types.contains(type)) {
                    // the reporter accepts this type
                    return reporter;
                } else {
                    // skip this one
                }
            } else {
                // reporter doesnt advertise - so skip it
            }
        }

        return null;
    }

    private void startLocalControllers() {
        Iterator<LocalControllerInfo> i = options_.getControllersIterator();
        Process child = null;

        while (i.hasNext()) {
            LocalControllerInfo lh = i.next();

            // try and see if we can talk to an exisiting LocalController
            boolean connected = false;
            try {
                // see if the LocalController already exists
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + " does LocalController " + lh + " already exist");
                LocalControllerInteractor inter = new LocalControllerInteractor(lh);
                connected = inter.checkLocalController(myHostInfo_);

            } catch (IOException iex) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " cannot connect to exisiting localController " + lh);
                connected = false;
            } catch (JSONException jex) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " cannot connect to exisiting localController " + lh);
                connected = false;
            }


            // Can't see it, so start a new one
            if (!connected) {

                String [] cmd = LocalControllerInitiator.localControllerStartCommand(lh, options_);
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
        interactorMap_ = new HashMap<LocalControllerInfo, LocalControllerInteractor>();
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
                LocalControllerInfo lcInfo = options_.getController(i);

                if (interactorMap_.get(lcInfo) == null) {
                    // we have not seen this LocalController before
                    // try and connect
                    try {
                        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Trying to make connection to "
                                                      + lcInfo.getName() + " " + lcInfo.getPort());
                        inter = new LocalControllerInteractor(lcInfo);

                        localControllers_.add(inter);
                        interactorMap_.put(lcInfo, inter);
                        boolean connected = inter.checkLocalController(myHostInfo_);

                        if (!connected) {
                            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot interact with LocalController " + lcInfo);
                            break;
                        } else {
                            aliveCount++;
                        }

                        if (options_.getRouterOptionsString() != "") {
                            inter.setConfigString(options_.getRouterOptionsString());
                        }

                        // tell the LocalController to start monitoring
                        // TODO: make more robust
                        // only work if address is real
                        // and/ or there is a consumer
                        if (latticeMonitoring) {
                            Logger.getLogger("log").logln(USR.STDOUT,
                                                          leadin() + "Setting  monitoring address: " + monitoringAddress + " timeout: " + monitoringTimeout);

                            inter.monitoringStart(monitoringAddress, monitoringTimeout);
                        }
                    } catch (Exception e) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Exception from " + lcInfo + ". " + e.getMessage());
                        //e.printStackTrace();
                        //shutDown();
                        //return;
                    }
                }
            }

            // check if we have connected to all of them
            // check if the no of controllers == the no of interactors
            // if so, we dont have to do all lopps
            if (noControllers_ == localControllers_.size()) {
                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + "All LocalControllers connected after " + (tries + 1) + " tries");
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

        /*
         * sclayman 20131209 - not sure this is needed
         *
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

        */

        // alternate
        if (aliveCount == noControllers_) {
            return;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Only " + aliveCount
                                          + " from " + noControllers_ + " local Controllers responded.");
            shutDown();
            return;
        }
    }


    /**
     * An alive message has been received from the host specified
     * in LocalHostInfo.
     * sclayman 20131209 - not sure this is needed
     *
     public void aliveMessage(LocalHostInfo lh) {
     aliveCount += 1;
     Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Received alive count from " + lh.getName() + ":" + lh.getPort());
     }
    */

    /**
     * Get the simulation start time.
     * This is the time the simulation actually started.
     */
    public long getStartTime() {
        if (options_.isSimulation()) {
            return 0;
        } else {
            if (scheduler_ == null) {
                return 0;
            } else {
                return scheduler_.getStartTime();
            }
        }
    }

    /**
     * Get the time since the simulation started
     */
    public long getElapsedTime() {
        if (isSimulation()) {
            return scheduler_.getElapsedTime();
        } else {
            if (scheduler_ == null) {
                return 0;
            } else {
                return System.currentTimeMillis() - scheduler_.getStartTime();
            }
        }
    }

    /** Gets the current time (From clock or the time of the last simulation event*/
    public long getTime() {
        if (isSimulation()) {
            return scheduler_.getElapsedTime();
        } else {
            if (scheduler_ == null) {
                return 0;
            } else {
                return System.currentTimeMillis();
            }
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
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Shutdown LocalController " + inter);
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
    public List<Integer> getAPs() {
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

    public JSONObject setAP(long time, int gid, int AP) {
        // this should be how we do it, but it is often called
        // from the middle of an event execution
        // therefore it is locked out as the existing event 
        // gets the semaphore
        /*
          try {
          SetAggPointEvent ev = new SetAggPointEvent(time, null, gid, AP);
          JSONObject jsobj = executeEvent(ev);
          return jsobj;
          } catch (Exception e) {
          return null;
          }
        */

        // print out a message
        if (gid == AP) {
            Logger.getLogger("log").logln(1<<8, elapsedToString(getElapsedTime()) + ANSI.BLUE + " ROUTER " + gid + " BECOME AP" + ANSI.RESET_COLOUR);
        } else {
            Logger.getLogger("log").logln(1<<8, elapsedToString(getElapsedTime()) + ANSI.CYAN + " ROUTER " + gid + " SET AP " + AP + ANSI.RESET_COLOUR);
        }




        try {
            SetAggPointEvent ev = new SetAggPointEvent(time, null, gid, AP);
            scheduler_.addEvent(ev);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void registerAggPoint(long time, int gid, int AP) {
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


    private JSONObject listAggPointsAsJSON() throws JSONException {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();

        for (Integer apID : getAPs()) {
            array.put(apID);
        }

        jsobj.put("type", "ap");
        jsobj.put("list", array);

        return jsobj;
    }

    private void addEvent(Event e) {
        scheduler_.addEvent(e);
    }

    /** Router GID reports a connection to access point AP */
    public boolean reportAP(int gid, int AP) {
        throw new Error("REMOVED 20140104 - sclayman");
    }

    public boolean isLatticeMonitoring() {
        return latticeMonitoring;
    }

    /**
     * Get the maximum lag this controller will allow.
     */
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
    public List<Integer> getRouterList() {
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

    /* Return a list of outlinks from a router */
    public List<Integer> getOutLinks(int routerId) {
        return network_.asList(network_.getOutLinks(routerId));
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

    public EventScheduler getEventScheduler()
    {
        return scheduler_;
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String GC = "GC: ";

        return getName() + " " + GC;
    }


    /*
     * Vim Functions
     */

    public JSONObject createRouter() throws JSONException {
        return createRouter("", "");
    }

    public JSONObject createRouter(String name, String address) throws JSONException {
        StartRouterEvent ev = new StartRouterEvent(getElapsedTime(), null, address, name);
        return executeEvent(ev);
    }

    public JSONObject createRouterWithName(String name) throws JSONException {
        return createRouter(name, "");
    }

    public JSONObject createRouterWithAddress(String address) throws JSONException {
        return createRouter("", address);
    }

    public JSONObject deleteRouter(int routerID) throws JSONException {
        EndRouterEvent ev = new EndRouterEvent(getElapsedTime(), null, routerID);
        return executeEvent(ev);
    }

    public JSONObject listRouters() throws JSONException {
        return listRouters("detail=id");
    }

    public JSONObject listLocalControllers() throws JSONException {
        return listLocalControllers("detail=id");
    }

    public JSONObject listRouters(String arg) throws JSONException {
        String detail = null;
        String value = null;
        JSONObject jsobj;

        if (arg == null || arg.equals("")) {
            detail = "id";

        } else if (!arg.contains("=")) {
            detail = arg;

        } else if (arg.startsWith("detail=")) {
            String[] parts = arg.split("=");
            detail = parts[1];

        } else if (arg.startsWith("name=")) {
            String[] parts = arg.split("=");
            value = parts[1];

        } else if (arg.startsWith("address=")) {
            String[] parts = arg.split("=");
            value = parts[1];

        } else {
            detail = "id";
        }

        if (detail != null) {
            jsobj = getAllRouterInfoAsJSON(detail);
        } else {
            jsobj = getOneRouterInfoAsJSON(value);
        }

        return jsobj;
    }

    public JSONObject listLocalControllers(String arg) throws JSONException {
        String detail = null;
        String value = null;
        JSONObject jsobj;

        if (arg == null || arg.equals("")) {
            detail = "name";

        } else if (!arg.contains("=")) {
            detail = arg;

        } else if (arg.startsWith("detail=")) {
            String[] parts = arg.split("=");
            detail = parts[1];

        } else if (arg.startsWith("name=")) {
            String[] parts = arg.split("=");
            value = parts[1];

        } else if (arg.startsWith("address=")) {
            String[] parts = arg.split("=");
            value = parts[1];

        } else {
            detail = "name";
        }

        if (detail != null) {
            jsobj = getAllLocalControllerInfoAsJSON(detail);
        } else {
            jsobj = getOneLocalControllerInfoAsJSON(value);
        }

        return jsobj;
    }

    public JSONObject listRemovedRouters() throws JSONException {
        return listShutdownRoutersAsJSON();
    }

    public JSONObject getRouterInfo(int id) throws JSONException {
        return findRouterInfoAsJSON(id);
    }

    public JSONObject getLocalControllerInfo(String name) throws JSONException {
        return getOneLocalControllerInfoAsJSON(name);
    }

    public JSONObject getRouterLinkStats(int id) throws JSONException {
        return getRouterLinkStatsAsJSON(id);
    }

    public JSONObject getRouterLinkStats(int id, int dstID) throws JSONException {
        return getRouterLinkStatsAsJSON(id, dstID);
    }

    public JSONObject getRouterCount() throws JSONException {
        int count = getNoRouters();

        JSONObject jsobj = new JSONObject();

        jsobj.put("value", count);

        return jsobj;

    }

    public JSONObject getMaxRouterID() throws JSONException {
        int maxid = getMaxRouterId();

        JSONObject jsobj = new JSONObject();

        jsobj.put("value", maxid);

        return jsobj;
    }

    public JSONObject createLink(int routerID1, int routerID2) throws JSONException {
        StartLinkEvent sle = new StartLinkEvent(getElapsedTime(), null, routerID1, routerID2);
        sle.setWeight(1);

        return executeEvent(sle);
    }

    public JSONObject createLink(int routerID1, int routerID2, int weight) throws JSONException {
        StartLinkEvent sle = new StartLinkEvent(getElapsedTime(), null, routerID1, routerID2);
        sle.setWeight(weight);

        return executeEvent(sle);

    }

    public JSONObject createLink(int routerID1, int routerID2, int weight, String linkName) throws JSONException {
        StartLinkEvent sle = new StartLinkEvent(getElapsedTime(), null, routerID1, routerID2);
        sle.setWeight(weight);
        sle.setName(linkName);

        return executeEvent(sle);
    }

    public JSONObject deleteLink(int linkID) throws JSONException {
        int router1, router2;

        // now lookup all the saved link info details
        LinkInfo li = findLinkInfo(linkID);
        router1 = li.getEndPoints().getFirst();
        router2 = li.getEndPoints().getSecond();

        EndLinkEvent ele = new EndLinkEvent(getElapsedTime(), null, router1, router2);

        return executeEvent(ele);
    }

    public JSONObject listLinks() throws JSONException {
        return listLinks("detail=id");
    }

    public JSONObject listLinks(String arg) throws JSONException {
        String detail = null;
        JSONObject jsobj;

        if (arg == null || arg.equals("")) {
            detail = "id";

        } else if (!arg.contains("=")) {
            detail = arg;

        } else if (arg.startsWith("detail=")) {
            String[] parts = arg.split("=");
            detail = parts[1];

        } else {
            detail = "id";
        }

        jsobj = getAllLinkInfoAsJSON(detail);

        return jsobj;
    }


    public JSONObject getLinkInfo(int id) throws JSONException {
        return findLinkInfoAsJSON(id);
    }

    public JSONObject setLinkWeight(int linkID, int weight) throws JSONException {
        int router1, router2;

        // now lookup all the saved link info details
        LinkInfo li = findLinkInfo(linkID);

        router1 = li.getEndPoints().getFirst();
        router2 = li.getEndPoints().getSecond();

        SetLinkWeightEvent slwe = new SetLinkWeightEvent(getElapsedTime(), null, router1, router2, weight); 

        return executeEvent(slwe);
    }

    public JSONObject getLinkCount() throws JSONException {
        int count = getNoLinks();

        JSONObject jsobj = new JSONObject();

        jsobj.put("value", count);

        return jsobj;
    }


    public JSONObject listRouterLinks(int routerID) throws JSONException {
        return listRouterLinks(routerID, "attr=id");
    }


    public JSONObject listRouterLinks(int rid, String arg) throws JSONException {
        String attr = null;
        JSONObject jsobj;

        if (arg == null || arg.equals("")) {
            attr = "id";

        } else if (!arg.contains("=")) {
            attr = arg;

        } else if (arg.startsWith("attr=")) {
            String[] parts = arg.split("=");
            attr = parts[1];

        } else {
            attr = "id";
        }

        return listRouterLinksAsJSON(rid, attr);

    }

    public JSONObject getRouterLinkInfo(int routerID, int linkID) throws JSONException {
        return findLinkInfoAsJSON(linkID);
    }

    public JSONObject createApp(int routerID, String className, String[] args) throws JSONException {
        StartAppEvent ase = new StartAppEvent(getElapsedTime(), null, routerID, className, args);
        return executeEvent(ase);
    }

    public JSONObject createApp(int routerID, String className, String rawArgs) throws JSONException {
        String[] args = null;
        // now convert raw args to String[]
        args = rawArgs.split(" ");

        StartAppEvent ase = new StartAppEvent(getElapsedTime(), null, routerID, className, args);
        return executeEvent(ase);
    }

    public JSONObject stopApp(int routerID, int appID) throws JSONException {
        StopAppEvent ase = new StopAppEvent(getElapsedTime(), null, routerID, appID);
        return executeEvent(ase);

    }

    public JSONObject listApps(int routerID) throws JSONException {
        return getAllAppInfoAsJSON(routerID);
    }

    public JSONObject getAppInfo(int routerID, int appID) throws JSONException {
        return findAppInfoAsJSON(appID);
    }

    public JSONObject listAggPoints() throws JSONException {
        return listAggPointsAsJSON();
    }


    public JSONObject getAggPointInfo(int id) throws JSONException {
        int ap = getAP(id);

        JSONObject jsobj = new JSONObject();

        jsobj.put("routerID", id);
        jsobj.put("ap", ap);

        return jsobj;
    }


    public JSONObject setAggPoint(int apID, int routerID) throws JSONException {
        setAP(getElapsedTime(),routerID, apID);


        JSONObject jsobj = new JSONObject();

        jsobj.put("routerID", routerID);
        jsobj.put("ap", apID);

        return jsobj;

    }



}


