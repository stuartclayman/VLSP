package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import usr.logging.*;
import usr.console.*;
import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import usr.common.*;
import usr.engine.*;
import java.util.concurrent.*;
import java.nio.channels.FileChannel;
import usr.interactor.*;
import usr.APcontroller.*;
import usr.router.*;
import usr.output.*;
import us.monoid.web.*;
import us.monoid.json.*;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneConsumerWithNames;
import eu.reservoir.monitoring.appl.BasicConsumer;
import java.net.InetSocketAddress;
import java.lang.reflect.Constructor;

/**
 * The GlobalController is in overall control of the software.  It
 * contacts LocalControllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
public class GlobalController implements ComponentController {
    private long simulationTime;   // Current time in simulation
    private long simulationStartTime;  // time at which simulation started  (assuming realtime)
    private long eventTime;            // The time of the current event
    private long lastEventLength;   // length of time previous event took
    private String xmlFile_;          // name of XML file containing config
    private LocalHostInfo myHostInfo_;  // Information about the local info
    private ControlOptions options_;   // Options affecting the simulation
    private boolean listening_;                         // Is this
    private GlobalControllerManagementConsole console_= null;
    private ArrayList <LocalControllerInteractor> localControllers_ = null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;
    private ArrayList <String> childNames_= null;  // names of child processes


    // Arrays below are sparse and this approach is for fast access.
    // outLinks_.get(i) returns a primitive array containing all the
    // nodes directly connected from node i
    private ArrayList <int []> outLinks_= null;  // numbers of nodes which are connected from
    // a given node
    private ArrayList <int []> linkCosts_= null;        // costs of connections in above

    private ArrayList <Integer> routerList_= null;   // List of integers which
    // contains the numbers of nodes present

    // Map connections LocalControllerInfo for given LCs to the appropriate interactors
    private HashMap <LocalControllerInfo, LocalControllerInteractor> interactorMap_= null;
    // Map is used to store vacant ports on local controllers
    private HashMap <LocalControllerInfo, PortPool> portPools_= null;


    // Map is from router Id to information one which machine router is stored on.
    private HashMap <Integer, BasicRouterInfo> routerIdMap_= null;

    // A map of routerID links to LinkInfo objects
    private HashMap<Integer, LinkInfo> linkInfo = null; 

    // A Map if appID to routerID
    // i.e the router the app is running on
    private HashMap<Integer, Integer> appInfo = null;

    private int aliveCount= 0; // Counts number of live nodes running.

    private EventScheduler scheduler_= null;
    // Class holds scheduler for event list

    private boolean simulationRunning_= true;
    // Used to stop simulation

    // Maximum ID no of any router instantiated so far.  Next router
    // will have number maxRouterId_+1
    private int maxRouterId_=0;

    private int noLinks_=0;  // number of links in network

    // Options structure which is given to each router.
    private RouterOptions routerOptions_= null;

    // Synchronization object used to wait for next event.
    private Object waitCounter_= null;

    // Variables relate to traffic output of statistics
    private ArrayList <OutputType> trafficOutputRequests_= null;
    private String routerStats_= "";
    private int statsCount_= 0;
    private ArrayList <Long> trafficOutputTime_= null;
    private HashMap<String, int []> trafficLinkCounts_ = null;


    private Thread ProcessOutputThread_;

    // Number of aggregation point controllers
    private int noControllers_= 0;

    // Thread name
    private String myName = "GlobalController";

    // Controller assigns aggregation points
    private APController APController_= null;

    // Used in shut down routines
    private boolean isActive = false;

    // Doing Lattice monitoring ?
    boolean latticeMonitoring = false;

    // A monitoring address
    InetSocketAddress monitoringAddress;
    int monitoringPort = 22997;
    int monitoringTimeout = 1;


    // A BasicConsumer for the stats of a Router
    BasicConsumer dataConsumer;

    // and the Reporters that handle the incoming measurements
    // Label -> Reporter
    HashMap<String, Reporter> reporterMap;

    // A Semaphore to have single access to some operations
    Semaphore semaphore;


    /**
     * Main entry point.
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Command line must specify "+
                               "XML file to read and nothing else.");
            System.exit(-1);
        }
        GlobalController gControl = new GlobalController();
        gControl.xmlFile_= args[0];
        gControl.init();

        if (gControl.getOptions().isSimulation()) {
            gControl.simulateSoftware();
        } else {
            gControl.simulateHardware();
        }
        Logger.getLogger("log").logln(USR.STDOUT, gControl.leadin() + "Simulation complete");
        System.out.flush();
    }

    /**
     * Construct a GlobalController -- this constructor contains things
       which apply whether we are simulation or emulation
     */
    public GlobalController () {
    }

    /** Basic intialisation for the global controller */
    private void init() {
        // allocate a new logger
        Logger logger = Logger.getLogger("log");
        // tell it to output to stdout
        // and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.STDOUT set

        // tell it to output to stderr
        // and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.ERROR set
        logger.addOutput(System.err, new BitMask(USR.ERROR));
        logger.addOutput(System.out, new BitMask(USR.STDOUT));
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Hello");

        // add some extra output channels, using mask bit 7, 8, 9, 10
        try {
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel7.out")), new BitMask(1<<7));
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel8.out")), new BitMask(1<<8));
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel9.out")), new BitMask(1<<9));
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel10.out")), new BitMask(1<<10));
        } catch (Exception e) {
            e.printStackTrace();
        }

        outLinks_= new ArrayList<int []> ();
        linkCosts_= new ArrayList<int []> ();
        routerList_= new ArrayList<Integer>();
        linkInfo = new HashMap<Integer, LinkInfo>();
        appInfo = new HashMap<Integer, Integer>();
        options_= new ControlOptions(xmlFile_);
        routerOptions_= options_.getRouterOptions();
        waitCounter_= new Object();

        // create a semaphore with 1 element to ensure
        // single access to some key code blocks
        semaphore = new Semaphore(1);

        // Redirect ouptut for error and normal output if requested in
        // router options file
        String fileName= routerOptions_.getOutputFile();
        if (!fileName.equals("")) {
            if (routerOptions_.getOutputFileAddName()) {
                fileName+= "_"+leadinFname();
            }
            File output= new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output,true);
                PrintWriter pw = new PrintWriter(fos,true);
                logger.removeOutput(System.out);
                logger.addOutput(pw, new BitMask(USR.STDOUT));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.err.println(fileName);
                System.exit(-1);
            }
        }
        String errorName= routerOptions_.getErrorFile();
        if (!errorName.equals("")) {
            if (routerOptions_.getOutputFileAddName()) {
                errorName+= "_"+leadinFname();
            }
            File output= new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output,true);
                PrintWriter pw = new PrintWriter(fos,true);
                logger.removeOutput(System.err);
                logger.addOutput(pw, new BitMask(USR.ERROR));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.err.println(fileName);
                System.exit(-1);
            }
        }


        // Set up AP controller
        APController_= ConstructAPController.constructAPController(routerOptions_);

        try {
            myHostInfo_= new LocalHostInfo(options_.getGlobalPort());
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());
            System.exit(-1);
        }

        if (options_.latticeMonitoring()) {
            latticeMonitoring = true;
        }

        if (latticeMonitoring) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Starting monitoring");

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
            startMonitoringConsumer(monitoringAddress);
        }


        // Set up simulations options
        if (!options_.isSimulation()) {
            initEmulation();
        }


        //Initialise events for schedules
        initSchedule();

        //Initialise output
        for (OutputType o : options_.getOutputs()) {
            if (o.clearOutputFile()) {
                File f= new File(o.getFileName());
                f.delete();
            }
        }
        isActive = true;
    }

    /**
       Initialisation if we are emulating on hardware.
     */
    private void initEmulation () {
        childProcessWrappers_ = new HashMap<String, ProcessWrapper>();
        childNames_= new ArrayList<String>();
        routerIdMap_= new HashMap<Integer, BasicRouterInfo>();
        console_= new GlobalControllerManagementConsole(this,myHostInfo_.getPort());
        console_.start();
        portPools_= new HashMap<LocalControllerInfo,PortPool>();
        noControllers_= options_.noControllers();
        LocalControllerInfo lh;
        for(int i= 0; i < noControllers_; i++) {
            lh = options_.getController(i);
            portPools_.put(lh,new PortPool(lh.getLowPort(),lh.getHighPort()));
        }
        if (options_.startLocalControllers()) {

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting Local Controllers");
            startLocalControllers();
        }
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Checking existence of local Controllers");
        checkAllControllers();
    }
    
    /** Main loop for events if software simulation */
    private void simulateSoftware() {
        long time= 0;
        while (simulationRunning_) {
            SimEvent e= scheduler_.getFirstEvent();
            if (e == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Out of events to execute");
                break;
            }
            simulationTime= e.getTime();
            executeEvent(e);
        }
    }

    /** Main loop for events if real time emulation */
    private void simulateHardware() {

        simulationStartTime = System.currentTimeMillis();
        while (simulationRunning_) {
            SimEvent e= scheduler_.getFirstEvent();
            if (e == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Out of events to execute!");
                break;
            }
            while(simulationRunning_) {
                eventTime= e.getTime();
                simulationTime= System.currentTimeMillis();

                if (simulationTime - (simulationStartTime + eventTime) > options_.getMaxLag()) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                                  "Simulation lagging too much, slow down events");
                    bailOut();
                    return;
                }

                if ((simulationStartTime + eventTime) <= simulationTime) {
                    executeEvent(e);
                    break;
                }


                if (checkMessages()) {
                    // Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Check msg true");
                    continue;
                }
                waitUntil(simulationStartTime + eventTime);
            }
        }
    }


    /**
     * Set up the reporters
     */
    private void setupReporters(HashMap<String, String>reporterInfoMap) {
        // skip through the map, instantiate a Probe and set its data rate
        for (Map.Entry<String, String> entry : reporterInfoMap.entrySet()) {
            String reporterClassName = entry.getValue();
            String label = entry.getKey();

            try {
                // Now convert the class name to a Class
                // get Class object
                // WAS Class<Reporter> cc = (Class<Reporter>)Class.forName(reporterClassName);

                // Replaced with following 2 lines
                Class<?> c = (Class<?>)Class.forName(reporterClassName);
                Class<? extends Reporter> cc = c.asSubclass(Reporter.class );

                // find Constructor for when arg is GlobalController
                Constructor<? extends Reporter> cons = (Constructor<? extends Reporter>)cc.getDeclaredConstructor(GlobalController.class);

                Reporter reporter =  (Reporter)cons.newInstance(this);
                reporterMap.put(label, reporter);

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Added reporter: " + reporter);

            } catch (ClassNotFoundException cnfe) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Class not found " + reporterClassName);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot instantiate class " + reporterClassName);
            }


        }
    }



    /** Check queued messages at Global Controller */
    private boolean checkMessages()
    {
        /* sclayman 6/6/2012
        BlockingQueue<Request> queue = console_.queue();
        if (queue.size() == 0)
            return false;
        Request req= queue.remove();
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "TODO -- need to deal with event here!");
        return true;
        */
        return false;
    }


    /** bail out of simulation relatively gracefully */
    private void bailOut() {
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Bailing out of run!");
        shutDown();
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Exit after bailout");
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Bailing out of run!");
    }

    private void executeEvent(SimEvent e) {
        Object extraParms= null;
        long eventBegin = System.currentTimeMillis();
        preceedEvent(e,scheduler_,this);
        Logger.getLogger("log").logln(USR.STDOUT, "EVENT: " + "<" + lastEventLength + "> " +
                                      e.getTime() + " @ " +
                                      eventBegin +  " => " +  e);

        int type= e.getType();
        try {
            long time= e.getTime();

            if (type == SimEvent.EVENT_START_SIMULATION) {
                startSimulation(time);
            }
            else if (type == SimEvent.EVENT_END_SIMULATION) {
                endSimulation(time);
            }
            else if (type == SimEvent.EVENT_START_ROUTER) {
                Object data = e.getData();

                if (data instanceof Pair) {
                    // there is just a Pair
                    Pair<?,?> pair= (Pair<?,?>)data;
                    String address = (String)pair.getFirst();
                    String name = (String)pair.getSecond();

                    // with specified address and name
                    startRouter(time, address, name);

                } else if (data instanceof String) {
                    String value = (String)e.getData();
                    // set value as name and address
                    startRouter(time, value, value);
                } else {
                    // no address, no name
                    startRouter(time, null, null);
                }
            }
            else if (type == SimEvent.EVENT_END_ROUTER) {
                Object arg = e.getData();
                int routerNo;

                if (arg instanceof Integer) {
                    routerNo = (Integer)arg;
                } else {
                    // arg is String, we need to look up the router IDs
                    String routerAddress = (String)arg;
                    BasicRouterInfo rInfo = findRouterInfo(routerAddress);

                    if (rInfo == null) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown router " + routerAddress +" in END_ROUTER at time " + time);
                        shutDown();
                    }

                    routerNo = rInfo.getId();

                }

                endRouter(time,routerNo);
            }
            else if (type == SimEvent.EVENT_START_LINK) {
                int router1= 0, router2= 0;

                Object data = e.getData();

                if (data instanceof Pair) {
                    // there is just a Pair
                    Pair<?,?> pair= (Pair<?,?>)data;
                    router1= (Integer)pair.getFirst();
                    router2= (Integer)pair.getSecond();
                    startLink(time,router1, router2, 1, null);

                } else {
                    // there is an Array of Objects
                    // this probably comes from a Script
                    Object[] array = (Object[])data;

                    // Process the link info
                    Pair<?,?> pair= (Pair<?,?>)array[0];

                    // process router spec
                    if (pair.getFirst() instanceof Integer && pair.getSecond() instanceof Integer) {
                        // there are 2 router ID's
                        router1= (Integer)pair.getFirst();
                        router2= (Integer)pair.getSecond();
                    } else {
                        // we need to look up the router IDs
                        String router1Address = (String)pair.getFirst();
                        String router2Address = (String)pair.getSecond();

                        BasicRouterInfo r1Info = findRouterInfo(router1Address);
                        BasicRouterInfo r2Info = findRouterInfo(router2Address);

                        if (r1Info == null) {
                            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown router " + router1Address +" in START_LINK at time " + time);
                            shutDown();
                        }

                        if (r2Info == null) {
                            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown router " + router2Address +" in START_LINK at time " + time);
                            shutDown();
                        }

                        router1 = r1Info.getId();
                        router2 = r2Info.getId();
                    }

                    if (array.length == 1) {
                        // there is just a Pair
                        startLink(time,router1, router2, 1, null);

                    } else if (array.length == 2) {
                        // there is a Pair and a weight
                        int weight = (Integer)array[1];
                        startLink(time,router1, router2, weight, null);
                    } else if (array.length == 3) {
                        // there is a Pair, a weight, and a name
                        int weight = (Integer)array[1];
                        String linkName = (String)array[2];
                        startLink(time,router1, router2, weight, linkName);
                    }

                }
            }

            else if (type == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();

                // process router spec
                if (pair.getFirst() instanceof Integer && pair.getSecond() instanceof Integer) {
                    // there are 2 router ID's
                    router1= (Integer)pair.getFirst();
                    router2= (Integer)pair.getSecond();
                } else {
                    // we need to look up the router IDs
                    String router1Address = (String)pair.getFirst();
                    String router2Address = (String)pair.getSecond();

                    BasicRouterInfo r1Info = findRouterInfo(router1Address);
                    BasicRouterInfo r2Info = findRouterInfo(router2Address);

                    if (r1Info == null) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown router " + router1Address +" in END_LINK at time " + time);
                        shutDown();
                    }

                    if (r2Info == null) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown router " + router2Address +" in END_LINK at time " + time);
                        shutDown();
                    }

                    router1 = r1Info.getId();
                    router2 = r2Info.getId();
                }


                endLink(time,router1, router2);
            }
            else if (type == SimEvent.EVENT_AP_CONTROLLER) {
                queryAPController(time);
            }
            else if (type == SimEvent.EVENT_OUTPUT) {
                produceOutput(time,(OutputType)(e.getData()));
            }
            else if (type == SimEvent.EVENT_ON_ROUTER) {
                runRouterEvent(time,e);

            } else if (type == SimEvent.EVENT_NEW_TRAFFIC_CONNECTION) {
                // Create a new traffic connection
                createTrafficConnection(time,e);

            }
            else {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unexected event type "
                                              +type+" shutting down!");
                shutDown();
                return;
            }
            long eventEnd = System.currentTimeMillis();
            lastEventLength = eventEnd - eventBegin;

        } catch (ClassCastException ex) {
            ex.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Event "+type+" had wrong object");
            shutDown();
            return;
        }
        followEvent(e,scheduler_,this, extraParms);

    }

    /** Event for start Simulation */
    private void startSimulation(long time) {

        lastEventLength = 0;
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Start of simulation event at: " +
                                      time+ " "+System.currentTimeMillis());
        for (OutputType o : options_.getOutputs()) {
            if (o.getTimeType() == OutputType.AT_START) {
                produceOutput(time,o);
            }
        }
    }

    /** Event for end Simulation */
    private void endSimulation(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "End of simulation event at " + time+ " "+System.currentTimeMillis());
        simulationRunning_= false;

        for (OutputType o : options_.getOutputs()) {
            if (o.getTimeType() == OutputType.AT_END) {
                produceOutput(time, o);
            }
        }
        shutDown();
    }

    /** Register existence of router */
    private void registerRouter(int rId)
    {
        routerList_.add(rId);
    }

    /** Event to start a router */
    protected int startRouter(long time, String address, String name) {
        // return +ve no for valid id
        // return -1 for no start - it's a simulation
        // return -2 for start fail
        // return -3 for semaphore not acquired in time
        // return -4 for acquire interrupted
        try {
            // wait forever for Semaphore
            // semaphore.acquire();

            // wait 2500ms to acquire the semaphore
            boolean acquired = semaphore.tryAcquire(2500, TimeUnit.MILLISECONDS);

            if (acquired) {
                maxRouterId_++;
                int rId= maxRouterId_;
                outLinks_.add(new int [0]);
                linkCosts_.add(new int [0]);
                registerRouter(rId);
                APController_.addNode(time, rId);
                if (!options_.isSimulation()) {
                    //System.err.println("Trying to start");
                    if (startVirtualRouter(maxRouterId_, address, name ) == false) {
                        //  System.err.println("Did not start");
                        unregisterRouter(rId);
                        return -2;
                    } else {
                        return rId;
                    }
                } else {
                    return -1;
                }
            } else {
                // not acquired
                return -3;
            }
        } catch (InterruptedException ie) {
            return -4;
        } finally {
            semaphore.release();
        }
    }

    private boolean startVirtualRouter(int id, String address, String name)
    {
        // Find least used local controller

        LocalControllerInfo lc;
        LocalControllerInfo leastUsed= options_.getController(0);
        double minUse= leastUsed.getUsage();
        double thisUsage;
        for (int i= 1; i < noControllers_; i++) {
            lc= options_.getController(i);
            thisUsage= lc.getUsage();
            // Logger.getLogger("log").logln(USR.STDOUT, i+" Usage "+thisUsage);
            if (thisUsage == 0.0) {
                leastUsed= lc;
                break;
            }
            if (thisUsage < minUse) {
                minUse= thisUsage;
                leastUsed= lc;
            }
        }
        if (minUse >= 1.0) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                          "Could not start new router on " + leastUsed+ " too many routers");
            //System.err.println("Too many routers");
            return false;
        }
        leastUsed.addRouter();  // Increment count
        LocalControllerInteractor lci= interactorMap_.get(leastUsed);


        int MAX_TRIES= 5;
        for (int i= 0; i < MAX_TRIES; i++) {
            try {
                if (tryRouterStart(id, address, name, leastUsed, lci)) {
                    //System.err.println("Started");
                    return true;
                }
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "Could not start new router on " + leastUsed+ " out of ports ");
                //System.err.println("Out of ports");
                return false;
            }
        }
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not start new router on "
                                      + leastUsed + " after "+MAX_TRIES+" tries.");
        //System.err.println("Could not start");
        return false;
    }

    /** Make one attempt to start a router */
    boolean tryRouterStart (int id, String address, String name, LocalControllerInfo local, LocalControllerInteractor lci)
    throws IOException {
        int port= 0;
        PortPool pp= portPools_.get(local);
        JSONObject routerAttrs;

        try {
            // find 2 ports
            port = pp.findPort(2);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Creating router: " + id + (address != null ? (" address = " + address) : "")  + (name != null ? (" name = " + name) : ""));

            // create the new router and get it's name
            routerAttrs = lci.newRouter(id, port, port+1, address, name);

            BasicRouterInfo br= new BasicRouterInfo((Integer)routerAttrs.get("routerID"), simulationTime,local, (Integer)routerAttrs.get("mgmtPort"), (Integer)routerAttrs.get("r2rPort"));
            br.setName((String)routerAttrs.get("name"));
            br.setAddress((String)routerAttrs.get("address"));
            // keep a handle on this router
            routerIdMap_.put(id,br);


            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Created router " + routerAttrs);
            Logger.getLogger("log").logln(1<<9, elapsedToString(getSimulationElapsedTime()) + ANSI.GREEN + " START ROUTER " + id + ANSI.RESET_COLOUR);


            return true;

        } catch (JSONException e) {
            // Failed to start#
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not create router " + id + " on "+lci);
            if (port != 0)
                pp.freePorts(port,port+1);  // Free ports but different ones will be tried next time
            return false;
        }

    }

    /** Event to end a router */
    protected int endRouter(long time, int routerId) {
        // return 0 for end of router
        // return -3 for semaphore not acquired in time
        // return -4 for acquire interrupted
        try {
            // wait forever for Semaphore
            // semaphore.acquire();

            // wait 2500ms to acquire the semaphore
            boolean acquired = semaphore.tryAcquire(2500, TimeUnit.MILLISECONDS);

            if (acquired) {

                if (options_.isSimulation()) {
                    endSimulationRouter(routerId);
                } else {
                    endVirtualRouter(routerId);
                }
                unregisterRouter(routerId);
                APController_.removeNode(time, routerId);

                return 0;
            } else {
                // not acquired
                return -3;
            }
        } catch (InterruptedException ie) {
            return -4;
        } finally {
            semaphore.release();
        }

    }


    /** Unregister a router and all links from structures in
        GlobalController*/
    private void unregisterRouter(int rId)
    {
        int[] out= getOutLinks(rId);
        //Logger.getLogger("log").logln(USR.ERROR, "Unregister router "+rId);
        for (int i= out.length-1; i >= 0; i--) {
            unregisterLink(rId,out[i]);
            // Logger.getLogger("log").logln(USR.ERROR, "Unregister link "+rId+" "+out[i]);

        }
        int index= routerList_.indexOf(rId);
        //Logger.getLogger("log").logln(USR.ERROR, "Router found at index "+index);
        routerList_.remove(index);
    }

    /** remove a router in simulation*/
    private void endSimulationRouter(int rId) {

    }

    /** Send shutdown to a virtual router */
    private void endVirtualRouter(int rId) {
        BasicRouterInfo br= routerIdMap_.get(rId);

        LocalControllerInteractor lci= interactorMap_.get(br.getLocalControllerInfo());
        int MAX_TRIES= 5;
        int i;
        for (i= 0; i < MAX_TRIES; i++) {
            try {
                lci.endRouter(br.getHost(),br.getManagementPort());

                Logger.getLogger("log").logln(1<<9, elapsedToString(getSimulationElapsedTime()) + ANSI.RED + " STOP ROUTER " + rId + ANSI.RESET_COLOUR);

                break;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+ "Cannot shut down router "+
                                              br.getHost()+":"+br.getManagementPort()+ " attempt "+(i+1) + " Exception = " + e);
            }
        }
        if (i == MAX_TRIES) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                          "Failed to shut down router " +
                                          br.getHost()+":"+br.getManagementPort());
            bailOut();
        }
        LocalControllerInfo lcinf= br.getLocalControllerInfo();
        PortPool pp= portPools_.get(lcinf);
        pp.freePort(br.getManagementPort());
        pp.freePort(br.getRoutingPort());
        lcinf.delRouter();
        routerIdMap_.remove(rId);

        // tell reporter that this router is gone
        if (latticeMonitoring) {
            String routerName = br.getName();
            
            for (Reporter reporter : reporterMap.values()) {
                if (reporter instanceof RouterDeletedNotification) {
                    ((RouterDeletedNotification)reporter).routerDeleted(routerName);
                }
            }
        }
    }

    /**
     * Find some router info
     */
    public BasicRouterInfo findRouterInfo(int rId) {
        return routerIdMap_.get(rId);
    }

    /**
     * Find some router info, given a router address or a router name
     */
    public BasicRouterInfo findRouterInfo(String value) {
        // skip through all the BasicRouterInfo objects
        for (BasicRouterInfo info : (Collection<BasicRouterInfo>)routerIdMap_.values()) {

            if (info.getAddress() != null && info.getAddress().equals(value)) {
                // we found a match
                return info;
            } else if (info.getName() != null && info.getName().equals(value)) {
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
     * Get the number of routers
     */
    public int getRouterCount() {
        return routerList_.size();
    }

    /**
     * Is the router ID valid.
     */
    public boolean isValidRouterID(int rId) {
        int index= routerList_.indexOf(rId);

        if (index >= 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find link info
     */
    public LinkInfo findLinkInfo(int linkID) {
        return linkInfo.get(linkID);
    }

    /**
     * List all LinkInfo
     */
    public Collection<LinkInfo> getAllLinkInfo() {
        return linkInfo.values();
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


    /** Register a link with structures necessary in Global
       Controller */
    private void registerLink(int router1Id, int router2Id)
    {
        noLinks_++;

        // Add links in both directions
        int [] out= getOutLinks(router1Id);
        int [] out2= new int [out.length +1];
        int [] costs= getLinkCosts(router1Id);
        int [] costs2= new int [out.length+1];
        System.arraycopy(out,0,out2,0,out.length);
        System.arraycopy(costs,0,costs2,0,out.length);
        out2[out.length]= router2Id;
        costs2[out.length]= 1; // Link cost 1 so far
        setOutLinks(router1Id, out2);
        setLinkCosts(router1Id, costs2);


        out= getOutLinks(router2Id);
        out2= new int [out.length +1];
        costs= getLinkCosts(router2Id);
        costs2= new int [out.length+1];
        System.arraycopy(out,0,out2,0,out.length);
        System.arraycopy(costs,0,costs2,0,out.length);
        out2[out.length]= router1Id;
        costs2[out.length]= 1; // Link cost 1 so far
        setOutLinks(router2Id, out2);
        setLinkCosts(router2Id, costs2);
    }

    /** Event to link two routers */
    protected int startLink(long time, int router1Id, int router2Id, int weight, String name) {
        // return +ve no for valid id
        // return -1 for no start - router at one end is not running
        // return -3 for semaphore not acquired in time
        // return -4 for acquire interrupted
        try {
            // wait forever for Semaphore
            // semaphore.acquire();

            // wait 2500ms to acquire the semaphore
            boolean acquired = semaphore.tryAcquire(2500, TimeUnit.MILLISECONDS);

            if (acquired) {
                //Logger.getLogger("log").logln(USR.ERROR, "Start link "+router1Id+" "+router2Id);

                int index= routerList_.indexOf(router1Id);
                if (index == -1)
                    return -1;  // Cannot start link as router 1 dead already
                index= routerList_.indexOf(router2Id);
                if (index == -1)
                    return -1;  // Cannot start link as router 2 dead already

                // check if this link already exists
                int [] outForRouter1 = getOutLinks(router1Id);

                boolean gotIt= false;
                for (int i : outForRouter1) {
                    if (i == router2Id) {
                        gotIt= true;
                        break;
                    }
                }

                if (gotIt) {             // we already have this link
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Link already exists: "+router1Id + " -> " + router2Id);
                    return 0;
                } else {
                    int linkID;

                    if (options_.isSimulation()) {
                        linkID = startSimulationLink(router1Id, router2Id);
                    } else {
                        linkID = startVirtualLink(router1Id, router2Id, weight, name);
                    }

                    // register inside GlobalController
                    registerLink(router1Id, router2Id);
                    // Tell APController about link
                    APController_.addLink(time, router1Id,router2Id);

                    return linkID;
                }
            } else {
                // not acquired
                return -3;
            }
        } catch (InterruptedException ie) {
            return -4;
        } finally {
            semaphore.release();
        }

    }

    /** Start simulation link */
    private int startSimulationLink(int router1Id, int router2Id) {
        return 0;
    }

    /**
     * Send commands to start virtual link
     * Args are: router1 ID, router2 ID, the weight for the link, a name for the link
     */
    private int startVirtualLink(int router1Id, int router2Id, int weight, String name) {

        BasicRouterInfo br1,br2;
        LocalControllerInfo lc;
        LocalControllerInteractor lci;


        br1= routerIdMap_.get(router1Id);
        br2= routerIdMap_.get(router2Id);
        if (br1 == null) {
            System.err.println ("Router "+router1Id+" does not exist when trying to link to "+ router2Id);
            return -1;
        }
        if (br2 == null) {
            System.err.println ("Router "+router2Id+" does not exist when trying to link to "+ router1Id);
            return -1;
        }
        //Logger.getLogger("log").logln(USR.STDOUT, "Got router Ids"+br1.getHost()+br2.getHost());

        lc= br1.getLocalControllerInfo();
        //Logger.getLogger("log").logln(USR.STDOUT, "Got LC");
        lci= interactorMap_.get(lc);
        //Logger.getLogger("log").logln(USR.STDOUT, "Got LCI");
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Global controller linking routers "+
                                      br1 + " and "+ br2);
        int MAX_TRIES= 5;
        int i;
        Integer linkID = -1;

        for (i=0; i < MAX_TRIES; i++) {
            try {
                String connectionName = lci.connectRouters(br1.getHost(), br1.getManagementPort(),
                                                           br2.getHost(), br2.getManagementPort(),
                                                           weight, name);

                // add Pair<router1Id, router2Id> -> connectionName to linkNames
                Pair<Integer, Integer> endPoints = makePair(router1Id, router2Id);
                linkID = endPoints.hashCode();

                linkInfo.put(linkID, new LinkInfo(endPoints, connectionName, weight, linkID));

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + br1 + " -> " + br2 + " = " + connectionName + " with link ID: " + linkID);

                Logger.getLogger("log").logln(1<<9, elapsedToString(getSimulationElapsedTime()) + ANSI.BLUE + " CREATE LINK " + router2Id + " TO " + router1Id + ANSI.RESET_COLOUR);

                break;
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "Cannot link routers "+router1Id+" "+router2Id+" try "+(i+1));
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());

            }
            catch (JSONException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "Cannot link routers "+router1Id+" "+router2Id+" try "+(i+1));
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            }
        }
        if (i == MAX_TRIES) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Giving up on linking");
            bailOut();
        }

        return linkID;

    }

    /* Return a list of outlinks from a router */
    public int [] getOutLinks(int routerId)
    {
        return outLinks_.get(routerId-1);
    }

    /* Return a list of outlinks from a router */
    public void setOutLinks(int routerId, int [] out)
    {
        outLinks_.set(routerId-1,out);
        //System.err.print(routerId+" contains ");
        //for (int i:out)
        //    System.err.print(i+" ");
        //System.err.println();
    }

    /**
     * Get the number of links
     */
    public int getLinkCount() {
        return noLinks_;
    }


    /**
     * Is a router directly connected to another one
     */
    public boolean isConnected(int routerId, int other) {
        int [] links = getOutLinks(routerId);

        for (int p=0; p<links.length; p++) {
            if (links[p] == other) {
                return true;
            }
        }


        return false;
    }

    /* Return a list of link costs from a router -- must be used in
        parallel get getOutLinks to id link nos*/
    public int [] getLinkCosts(int routerId)
    {
        return linkCosts_.get(routerId-1);

    }

    /* Return a list of link costs from a router -- must be used in
        parallel with setOutLinks to id link nos*/
    public void setLinkCosts(int routerId, int [] costs)
    {
        linkCosts_.set(routerId-1, costs);

    }

    /** Create pair of integers with first integer smallest */
    private Pair <Integer, Integer> makeRouterPair (int r1, int r2)
    {
        Pair <Integer, Integer> rpair;
        if (r1 < r2)
            rpair= new Pair<Integer,Integer>(r1,r2);
        else
            rpair= new Pair<Integer,Integer>(r2,r1);
        return rpair;
    }

    /** Create pair of integers  */
    private Pair <Integer, Integer> makePair (int r1, int r2)
    {
        return new Pair<Integer,Integer>(r1,r2);
    }

    /** Register a link with structures necessary in Global
       Controller */
    private void unregisterLink(int router1Id, int router2Id)
    {
        Pair <Integer, Integer> rnos= makeRouterPair(router1Id,router2Id);
        int [] out;
        int [] out2;
        int [] costs;
        int [] costs2;
        int arrayPos= 0;
        noLinks_--;
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Adding link from "+router1Id+" "+router2Id);
        // remove r2 from outlinks of r1
        out= getOutLinks(router1Id);
        costs= getLinkCosts(router1Id);
        for (int i= 0; i < out.length; i++) {
            if (out[i] == router2Id) {
                arrayPos= i;
                break;
            }
        }
        out2= new int[out.length -1];
        costs2= new int [out.length -1];
        System.arraycopy(out,0, out2,0, out.length-1);
        System.arraycopy(costs,0, costs2,0, out.length-1);
        //System.err.println("Old array "+out+" new "+out2+" remove "+router1Id+" "+router2Id+" pos "+arrayPos);

        if (arrayPos != out.length -1) {
            out2[arrayPos]= out[out.length-1];
            costs2[arrayPos]= costs[out.length-1];
        }
        setOutLinks(router1Id,out2);
        setLinkCosts(router1Id,costs2);
        // remove r1 from outlinks of r2
        out= getOutLinks(router2Id);
        costs= getLinkCosts(router2Id);
        for (int i= 0; i < out.length; i++) {
            if (out[i] == router1Id) {
                arrayPos= i;
                break;
            }
        }
        out2= new int[out.length -1];
        costs2= new int [out.length -1];
        System.arraycopy(out,0, out2,0, out.length-1);
        System.arraycopy(costs,0, costs2,0, out.length-1);
        if (arrayPos != out.length -1) {
            out2[arrayPos]= out[out.length-1];
            costs2[arrayPos]= costs[out.length-1];
        }
        setOutLinks(router2Id,out2);
        setLinkCosts(router2Id,costs2);

    }

    private void endSimulationLink(int router1Id, int router2Id)
    /** Event to end simulation link between two routers */
    {

    }

    /** Event to end virtual link between two routers */
    private void endVirtualLink(int rId1, int rId2) {

        BasicRouterInfo br1= routerIdMap_.get(rId1);
        BasicRouterInfo br2= routerIdMap_.get(rId2);
        LocalControllerInteractor lci= interactorMap_.get
                                           (br1.getLocalControllerInfo());
        int MAX_TRIES= 5;
        int i;
        for (i= 0; i < MAX_TRIES; i++) {
            try {
                // TODO:  work out link name, and pass that to LocalControllerInteractor
                lci.endLink(br1.getHost(), br1.getManagementPort(), br2.getAddress()); // rId2);

                // remove Pair<router1Id, router2Id> -> connectionName to linkNames
                Pair<Integer, Integer> pair = makePair(rId1, rId2);
                Integer linkID = pair.hashCode();
                linkInfo.remove(linkID);

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "remove link from: " + rId2 + " to " + rId1 + " with link ID: " + linkID);

                Logger.getLogger("log").logln(1<<9, elapsedToString(getSimulationElapsedTime()) + ANSI.MAGENTA + " REMOVE LINK " + rId2 + " TO " + rId1 + ANSI.RESET_COLOUR);

                break;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+ "Cannot shut down link "+
                                              br1.getHost()+":"+br1.getManagementPort()+" " +
                                              br2.getHost()+":"+br2.getManagementPort()+ " try "+(i+1) );
            }
        }
        if (i == MAX_TRIES) {
            Logger.getLogger("log").logln(USR.ERROR,"Giving up after failure to shut link");
            bailOut();
        }
    }

    /** Event to unlink two routers */
    protected int endLink (long time, int router1Id, int router2Id) {
        // return 0 for end of link
        // return -3 for semaphore not acquired in time
        // return -4 for acquire interrupted
        try {
            // wait forever for Semaphore
            // semaphore.acquire();

            // wait 2500ms to acquire the semaphore
            boolean acquired = semaphore.tryAcquire(2500, TimeUnit.MILLISECONDS);

            if (acquired) {


                unregisterLink(router1Id, router2Id);
                APController_.removeLink(time, router1Id,router2Id);
                if (options_.isSimulation()) {
                    endSimulationLink(router1Id, router2Id);
                } else {
                    endVirtualLink(router1Id, router2Id);
                }

                return 0;
            } else {
                // not acquired
                return -3;
            }
        } catch (InterruptedException ie) {
            return -4;
        } finally {
            semaphore.release();
        }


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
     * Run an application on a Router.
     * Returns the app ID
     */
    public int onRouter(int routerID, String className, String[] args) {
        return appStart(routerID, className, args);
    }


    /**
     * Run an application on a Router.
     * Returns the app ID
     */
    protected int appStart(int routerID, String className, String[] args) {
        // return +ve no for valid id
        // return -1 for no start - cant find LocalController
        // return -2 for no start - router cant run app
        // return -3 for semaphore not acquired in time
        // return -4 for acquire interrupted
        try {
            // wait forever for Semaphore
            // semaphore.acquire();

            // wait 2500ms to acquire the semaphore
            boolean acquired = semaphore.tryAcquire(2500, TimeUnit.MILLISECONDS);

            if (acquired) {
                BasicRouterInfo br = routerIdMap_.get(routerID);

                LocalControllerInteractor lci= interactorMap_.get(br.getLocalControllerInfo());

                if (lci == null) {
                    return -1;
                }
                int i;
                int MAX_TRIES= 5;
                Integer appID = -1;

                for (i=0; i < MAX_TRIES; i++) {
                    try {
                        // appStart returns a JSONObject
                        // something like: {"aid":1,"startTime":1340614768099,
                        // "name":"/R4/App/usr.applications.RecvDataRate/1"}                        


                        JSONObject response = lci.appStart(routerID, className, args);
                
                        // consturct an ID from the routerID and the appID
                        Pair<Integer, Integer> idPair = new Pair<Integer, Integer>(routerID, (Integer)response.get("aid"));
                
                        appID = idPair.hashCode();

                        String appName = (String)response.get("name");

                        // Add app to BasicRouterInfo
                        br.addApplication(appID, appName);

                        // and set info as
                        // ["id": 46346535, "time" : "00:14:52", "aid" : 1, "startime" : 1331119233159, "state": "RUNNING", "classname" : "usr.applications.Send", "args" : "[4, 3000, 250000, -d, 250, -i, 10]" ] 
                        Map<String, Object>dataMap = new HashMap<String, Object>();
                        dataMap.put("time", "00:00:00");
                        dataMap.put("id", appID);
                        dataMap.put("aid", (Integer)response.get("aid"));
                        dataMap.put("startime", (Long)response.get("startTime"));
                        dataMap.put("runtime", 0);
                        dataMap.put("classname", className);
                        dataMap.put("args", Arrays.asList(args).toString());
                        dataMap.put("state", "STARTED");

                        br.setApplicationData(appName, dataMap);

                        // add app to app info
                        appInfo.put(appID, routerID);

                        return appID;
                    } catch (Exception e) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                                      " failed to start app "+className+" on "+ routerID+ " try "+i + " with Exception " + e);

                    }
                }
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                              " failed to start app "+className+" on "+ routerID+ " giving up ");
                return -2;
            }  else {
                // not acquired
                return -3;
            }
        } catch (InterruptedException ie) {
            return -4;
        } finally {
            semaphore.release();
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
            Logger.getLogger("log").logln(USR.ERROR, leadin() +"Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        } catch (JSONException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        }
    }

    /**
     * Get the router stats -- method is blocking
     */
    public List<String> compileRouterStats()  {

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
            Logger.getLogger("log").logln(USR.ERROR, leadin() +"Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return null;
        } catch (JSONException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return null;
        }

    }

    /** Shutdown called from console -- add shut down command to list to
       happen now */
    public void shutDownCommand() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Shut down called from console");
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION,0,null,null);
        scheduler_.addEvent(e);
        wakeWait();
    }

    /*
     * Shutdown
     */
    void shutDown() {
        simulationRunning_= false;
        if (isActive) {

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "SHUTDOWN CALLED!");
            if (!options_.isSimulation()) {

                // stop monitoring
                if (latticeMonitoring) {
                    stopMonitoringConsumer();
                }

                //ThreadTools.findAllThreads("GC pre killAllControllers:");

                killAllControllers();

                //ThreadTools.findAllThreads("GC post killAllControllers:");

                while (checkMessages()) {};

                Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Pausing.");

                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin()+ e.getMessage());
                    System.exit(-1);
                }

                //ThreadTools.findAllThreads("GC post checkMessages:");

                Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Stopping console");
                console_.stop();

                //ThreadTools.findAllThreads("GC post stop console:");
            }
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "All stopped, shut down now!");

            isActive = false;
        }
    }

    /** Scheduled query of Aggregation point controller*/
    private void queryAPController(long time) {
        // Schedule next consider time
        SimEvent e= new SimEvent(SimEvent.EVENT_AP_CONTROLLER,
                                 time+routerOptions_.getControllerConsiderTime(), null,null);
        scheduler_.addEvent(e);
        APController_.controllerUpdate(time, this);

    }

    public void APControllerUpdate(long time)
    {
        APController_.controllerUpdate(time, this);
    }

    /** Produce some output */
    private void produceOutput(long time, OutputType o) {

        File f;
        FileOutputStream s= null;
        PrintStream p= null;
        try {
            f= new File(o.getFileName());
            s = new FileOutputStream(f,true);
            p = new PrintStream(s,true);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot open "+o.getFileName()+
                                          " for output "+e.getMessage());
            return;
        }
        o.makeOutput(time, p, this);
        // Schedule next output time
        if (o.getTimeType() == OutputType.AT_INTERVAL) {
            SimEvent e= new SimEvent(SimEvent.EVENT_OUTPUT,
                                     time+o.getTime(),o,null);
            scheduler_.addEvent(e);
        }
        p.close();
        try {
            s.close();
        } catch (IOException ex) {

        }
    }

    
    public HashMap<String, int []> getTrafficLinkCounts() 
    {
        return trafficLinkCounts_;
    }
    

    private void runRouterEvent(long time, SimEvent e)
    {
        String[] eventArgs = (String[])e.getData();
        Scanner sc = new Scanner(eventArgs[0]);

        int routerID;

        // process router spec
        if (sc.hasNextInt()) {
            // arg is int
            routerID = sc.nextInt();

        } else {
            // arg is String, we need to look up the router IDs
            String routerName = eventArgs[0].trim();
            BasicRouterInfo rInfo = findRouterInfo(routerName);

            if (rInfo == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown router " + routerName +" in ON_ROUTER at time " + time);
                shutDown();
            }

            routerID = rInfo.getId();

        }

        String className = eventArgs[1];

        // eliminate router_id and className
        String[] cmdArgs = new String[eventArgs.length-2];

        for (int a=2; a < eventArgs.length; a++) {
            cmdArgs[a-2] = eventArgs[a];
        }

        // call appStart with correct args
        appStart(routerID, className, cmdArgs);
    }

    /** Create a connection to send/receive data between two sites */
    private void createTrafficConnection(long time, SimEvent e)
    {
        if (options_.isSimulation()) {
            Logger.getLogger("log").logln(USR.ERROR,"Traffic options not available in simulation");
            return;
        }
        int nRouters= getNoRouters();
        if (nRouters < 2)
            return;
        int from;
        int to;
        BackgroundTrafficEngine eng= (BackgroundTrafficEngine)e.getEngine();
        if (eng == null) {
            Logger.getLogger("log").logln(USR.ERROR,"Need background traffic engine to gen traffic");
            bailOut();
        }
        if (eng.preferEmptyNodes() == true) {
            Logger.getLogger("log").logln(USR.ERROR,"Not written engine to prefer empty nodes");
            bailOut();
        }
        from= (int)Math.floor(Math.random()*nRouters);
        to= (int)Math.floor(Math.random()*(nRouters-1));
        int toRouter= routerList_.get(to);
        int fromRouter= routerList_.get(from);
        if (to == from)
            to= nRouters-1;
        String [] fromArgs= new String[4];
        String [] toArgs= new String[2];
        String port= ((Integer)eng.getReceivePort(to)).toString();
        String bytes= ((Integer)eng.getBytes()).toString();
        String rate= ((Double)eng.getRate()).toString();
        fromArgs[0]= routerIdMap_.get(toRouter).getAddress();
        fromArgs[1]= port;
        toArgs[0]= port;
        fromArgs[2]= bytes;
        toArgs[1]= bytes;
        fromArgs[3]= rate;
        appStart(toRouter,"usr.applications.Receive",toArgs);
        appStart(fromRouter,"usr.applications.Transfer",fromArgs);
    }

    /** When output for traffic is requested then queue requests for traffic from
     * routers */
    public void checkTrafficOutputRequests(long time, OutputType o)
    {
        if (options_.isSimulation()) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                          "Request for output of traffic makes sense only in context of emulation");
            return;
        }
        if (trafficOutputRequests_ == null) {
            trafficOutputRequests_= new ArrayList<OutputType>();
            trafficOutputTime_= new ArrayList<Long>();
        }
        trafficOutputRequests_.add(o);
        trafficOutputTime_.add(time);
        /** If requests already sent then just add it to the output request queue
           rather than sending a fruther request */
        if (trafficOutputRequests_.size() > 1) {
            return;
        }
        //  Make request for stats
        requestRouterStats();
    }
    
    /** Receiver router traffic -- if it completes a set then output it */
    public void receiveRouterStats(String stats)
    {
        synchronized (routerStats_) {
            statsCount_++;

            routerStats_= routerStats_.concat(stats);
            // System.err.println("Stat count is "+statsCount_);
            if (statsCount_ < localControllers_.size()) // Not got all stats yet
                return;
            //System.err.println("Enough"+routerStats_);
            File f;
            FileOutputStream s=null;
            PrintStream p=null;
            for (int i= 0; i < trafficOutputRequests_.size(); i++) {
                OutputType o = trafficOutputRequests_.get(i);
                try {
                    f= new File(o.getFileName());
                    s = new FileOutputStream(f,true);
                    p = new PrintStream(s,true);
                } catch (Exception e) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot open "+o.getFileName()+
                                                  " for output "+e.getMessage());
                    return;
                }
                OutputTraffic ot= (OutputTraffic)o.getOutputClass();
                ot.produceOutput(trafficOutputTime_.get(i),p, o,this);
            }
            //    System.err.println("Requests done");
            trafficOutputRequests_= new ArrayList<OutputType>();
            trafficOutputTime_= new ArrayList<Long>();
            statsCount_= 0;
            routerStats_= "";
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

        // check to see if the monitoring is already connected and running
        if (dataConsumer.isConnected()) {
            // if it is, stop it first
            stopMonitoringConsumer();
        }

        // set up DataPlane
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
    public synchronized void stopMonitoringConsumer() {
        if (dataConsumer.isConnected()) {
            dataConsumer.clearReporters();  // was setReporter(null);

            dataConsumer.disconnect();
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
    public Reporter findByInterface(Class inter) {
        // skip through each Reporter
        for (Reporter reporter :  reporterMap.values()) {
            // skip through each Interface
            for (Class<?>rI : reporter.getClass().getInterfaces()) {

                if (rI.isAssignableFrom(inter)) {
                    return reporter;
                }

                /*
                if (inter.isAssignableFrom(rI)) {
                    return reporter;
                }
                */
            }

        }

        return null;
    }


    private void initSchedule() {
        scheduler_= new EventScheduler();
        options_.initialEvents(scheduler_,this);

    }


    private void startLocalControllers() {
        Iterator i= options_.getControllersIterator();
        Process child= null;

        while (i.hasNext()) {

            LocalControllerInfo lh= (LocalControllerInfo)i.next();
            String [] cmd= options_.localControllerStartCommand(lh);
            try {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting process " + Arrays.asList(cmd));
                child = new ProcessBuilder(cmd).start();
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unable to execute remote command "+ Arrays.asList(cmd));
                Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
                System.exit(-1);
            }

            String procName = lh.getName()+":"+lh.getPort();
            childNames_.add(procName);
            childProcessWrappers_.put(procName, new ProcessWrapper(child, procName));

            try {
                Thread.sleep(100);  // Simple wait is to
                // ensure controllers start up
            }
            catch (java.lang.InterruptedException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "initVirtualRouters Got interrupt!");
                System.exit(-1);
            }

        }
    }

    /**
     * An alive message has been received from the host specified
     * in LocalHostInfo.
     */
    public void aliveMessage(LocalHostInfo lh)
    {
        aliveCount+= 1;
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Received alive count from "+
                                      lh.getName()+":"+lh.getPort());
    }

    /**
     * Wait until a specified absolute time is milliseconds.
     */
    public void waitUntil(long time){
        long now = System.currentTimeMillis();

        if (time <= now)
            return;
        try {
            long timeout = time - now;

            Logger.getLogger("log").logln(USR.STDOUT, "EVENT: " +  "<" + lastEventLength + "> " +
                                          (now - simulationStartTime) + " @ " +
                                          now +  " waiting " + timeout);
            synchronized (waitCounter_) {
                waitCounter_.wait(timeout);
            }
            lastEventLength = System.currentTimeMillis() - now;
        } catch(InterruptedException e) {
            checkMessages();
        }
    }

    /** Interrupt above wait*/
    public void wakeWait() {
        synchronized (waitCounter_) {
            waitCounter_.notify();
        }
    }


    /**
     * Get the simulation start time.
     * This is the time the simulation actually started.
     */
    public long getSimulationStartTime() {
        return simulationStartTime;
    }

    /**
     * Get the simulation current time.
     * This is the current time within the simulation.
     */
    public long getSimulationCurrentTime() {
        return simulationTime;
    }

    /**
     * Get the simulation elapsed time.
     * This is the elapsed time within the simulation.
     */
    public long getSimulationElapsedTime() {
        return simulationTime - simulationStartTime;
    }

    /**
     * Get the time of the current event
     */
    public long getEventTime() {
        return eventTime;
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
    public ManagementConsole getManagementConsole() {
        return console_;
    }


    /**Accessor function for maxRouterId_*/
    public int getMaxRouterId() {
        return maxRouterId_;
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
        localControllers_= new ArrayList<LocalControllerInteractor>();
        interactorMap_= new HashMap<LocalControllerInfo, LocalControllerInteractor>();
        LocalControllerInteractor inter= null;

        // lopp a bit and try and talk to the LocalControllers
        for (tries = 0; tries < MAX_TRIES; tries++) {
            // sleep a bit
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
            }

            // visit every LocalController
            for (int i= 0; i < noControllers_; i++) {
                LocalControllerInfo lh= options_.getController(i);

                if (interactorMap_.get(lh) == null) {
                    // we have not seen this LocalController before
                    // try and connect
                    try {
                        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Trying to make connection to "+
                                                      lh.getName()+" "+lh.getPort());
                        inter= new LocalControllerInteractor(lh);

                        localControllers_.add(inter);
                        interactorMap_.put(lh,inter);
                        inter.checkLocalController(myHostInfo_);

                        if (options_.getRouterOptionsString() != "") {
                            inter.setConfigString(options_.getRouterOptionsString());
                        }

                        // tell the LocalController to start monitoring
                        // TODO: make more robust
                        // only work if address is real
                        // and/ or there is a consumer
                        if (latticeMonitoring) {
                            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Setting  monitoring address: "+ monitoringAddress + " timeout: "  + monitoringTimeout);
                            inter.monitoringStart(monitoringAddress, monitoringTimeout);
                        }
                    } catch (Exception e) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Exception from " + lh + ". " + e.getMessage());
                        e.printStackTrace();
                        bailOut();
                        return;
                    }

                }
            }

            // check if we have connected to all of them
            // check if the no of controllers == the no of interactors
            // if so, we dont have to do all lopps
            if (noControllers_ == localControllers_.size()) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "All LocalControllers connected after " + (tries+1) + " tries");
                isOK = true;
                break;
            }
        }

        // if we did all loops and it's not OK
        if (!isOK) {
            // couldnt reach all LocalControllers
            // We can keep a list of failures if we need to.
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Can't talk to all LocalControllers");
            bailOut();
            return;
        }


        // Wait to see if we have all controllers.
        for (int i= 0; i < options_.getControllerWaitTime(); i++) {
            while (checkMessages()) {};
            if (aliveCount == noControllers_) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "All controllers responded with alive message.");
                return;
            }
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {

            }

        }
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Only "+aliveCount+" from "+noControllers_+
                                      " local Controllers responded.");
        bailOut();
        return;
    }

    /**
     * Send shutdown message to all controllers
     */
    private void killAllControllers() {
        if (localControllers_ == null)
            return;
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping all controllers");
        LocalControllerInteractor inter;
        for (int i= 0; i < localControllers_.size(); i++) {
            inter= localControllers_.get(i);
            try {

                inter.shutDown();

                //ThreadTools.findAllThreads("GC post kill :" + i);

            } catch (IOException e) {
                System.err.println (leadin() + "Cannot send shut down to local Controller");
                System.err.println (e.getMessage());
            } catch (JSONException e) {
                System.err.println (leadin() + "Cannot send shut down to local Controller");
                System.err.println (e.getMessage());
            }
        }
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stop messages sent to all controllers");

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping process wrappers");
        Collection <ProcessWrapper> pws= (Collection<ProcessWrapper>)childProcessWrappers_.values();
        for (ProcessWrapper pw : pws) {
            pw.stop();
        }

    }




    public int getLinkWeight(int l1, int l2)
    /** Return the weight from link1 to link2 or 0 if no link*/
    {
        int [] out=  getOutLinks(l1);
        int index= -1;
        for (int i= 0; i < out.length; i++) {
            if (out[i] == l2) {
                index= i;
                break;
            }
        }
        if (index == -1)
            return 0;
        int [] costs= getLinkCosts(l1);
        return costs[index];
    }




    public void setAP(int gid, int AP)
    {
        //System.out.println("setAP called");
        if (!simulationRunning_)
            return;
        Logger.getLogger("log").logln(USR.STDOUT,leadin()+" router "+gid+
                                      " now has access point "+AP);
        if (options_.isSimulation())
            return;
        BasicRouterInfo br= routerIdMap_.get(gid);
        if (br == null) {
            Logger.getLogger("log").logln(USR.ERROR,leadin()+" unable to find router "+gid+
                                          " in router map");
            return;
        }
        LocalControllerInteractor lci= interactorMap_.get
                                           (br.getLocalControllerInfo());
        if (lci == null) {
            Logger.getLogger("log").logln(USR.ERROR,leadin()+" unable to find router "+gid+
                                          " in interactor map");
            return;
        }
        try {
            lci.setAP(gid,AP);

            if (gid == AP) {
                Logger.getLogger("log").logln(1<<8, elapsedToString(getSimulationElapsedTime()) + ANSI.BLUE + " ROUTER " + gid + " BECOME AP" + ANSI.RESET_COLOUR);
            } else {
                Logger.getLogger("log").logln(1<<8, elapsedToString(getSimulationElapsedTime()) + ANSI.CYAN + " ROUTER " + gid + " SET AP " + AP + ANSI.RESET_COLOUR);
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,leadin()+" unable to set AP for router "+gid);
        }
    }

    /** Router GID reports a connection to access point AP */
    public boolean reportAP(int gid, int AP)
    {
        Logger.getLogger("log").logln(USR.ERROR,leadin()+"TODO write reportAP");
        return true;
    }


    /** Check network for isolated nodes and connect them if possible */
    public void checkIsolated(long time)
    {
        for (int i : getRouterList()) {
            checkIsolated(time,i);
        }
    }

    /** Check if given node is isolated and connect it if possible */
    public void checkIsolated(long time, int gid)
    {
        int [] links= getOutLinks(gid);
        int nRouters= routerList_.size();
        if (nRouters == 1) // One node is allowed to be isolated
            return;
        if (links.length > 0) {
            return;
        }
        // Node is isolated.
        while (true) {
            int i= (int)Math.floor( Math.random()*nRouters);
            int dest= routerList_.get(i);
            if (dest != gid) {
                startLink(time,gid,dest, 1, null);
                break;
            }
        }
    }

    /** Make sure network is connected*/
    public void connectNetwork(long time)
    {
        int nRouters= routerList_.size();
        if (nRouters <=1)
            return;
        // Boolean arrays are larger than needed but this is fast
        boolean [] visited= new boolean[maxRouterId_+1];
        boolean [] visiting= new boolean[maxRouterId_+1];
        for (int i= 0; i < maxRouterId_+1; i++) {
            visited[i]= true;
            visiting[i]= false;
        }
        for (int i= 0; i < nRouters; i++) {
            visited[routerList_.get(i)]= false;
        }
        int [] toVisit= new int[nRouters];
        int toVisitCtr= 1;
        toVisit[0]= routerList_.get(0);
        int noVisited= 0;
        while (noVisited < nRouters) {
            //System.err.println("NoVisited = "+noVisited+" / "+nRouters);
            if (toVisitCtr == 0) { // Not visited everything so make a new link
                // Choose i1 th visited and i2 th unvisited
                int i1= (int)Math.floor(Math.random()*noVisited);
                int i2= (int)Math.floor(Math.random()*(nRouters-noVisited));
                int l1= -1;
                int l2= -1;
                //System.err.println("Pick "+i1+" from "+noVisited+" visited nodes");
                //System.err.println("Pick "+i2+" from "+(nRouters-noVisited)+" unvisited nodes");
                for (int i= 0; i < nRouters; i++) {
                    int tmpNode= routerList_.get(i);
                    if (visited[tmpNode] && i1 >= 0) {

                        if (i1 == 0) {
                            l1= tmpNode;
                        }
                        i1--;
                    } else if (!visited[tmpNode] && i2 >= 0) {

                        if (i2 == 0) {
                            l2= tmpNode;
                        }
                        i2--;
                    }
                    if (i1 < 0 && i2 < 0)
                        break;
                }
                if (l1 == -1 || l2 == -1) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Error in network connection "+l1+" "+l2);
                    bailOut();
                    return;
                }
                // Make a new link to connect network
                //System.err.println("Link "+l1+" is "+visited[l1]+" "+l2+" is "+visited[l2]);
                startLink(time,l1,l2, 1, null);
                toVisitCtr++;
                toVisit[0]= l2;
            }
            toVisitCtr--;
            int node= toVisit[toVisitCtr];
            visited[node]= true;
            noVisited++;
            for (int l : getOutLinks(node)) {
                if (visited[l] == false && visiting[l] == false) {
                    toVisit[toVisitCtr]= l;
                    visiting[l]= true;
                    toVisitCtr++;
                }
            }
        }

    }

    /** Add or remove events following a simulation event */
    public void preceedEvent(SimEvent e, EventScheduler s, GlobalController g)
    {
        e.preceedEvent(s,g);
    }

    /** Add or remove events following a simulation event -- object allows
       global controller to pass extra parameters related to event if necessary*/
    public void followEvent(SimEvent e, EventScheduler s, GlobalController g,
                            Object o)
    {
        e.followEvent(s,g,o);
        long time= e.getTime();
        if (options_.connectedNetwork()) {
            if (e.getType() == SimEvent.EVENT_END_ROUTER) {
                g.connectNetwork(time);
            } else if (e.getType() == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                g.connectNetwork(time,router1, router2);
            }
        } else if (!options_.allowIsolatedNodes()) {
            if (e.getType() == SimEvent.EVENT_END_ROUTER) {
                g.checkIsolated(time);
            } else if (e.getType() == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                g.checkIsolated(time,router1);
                g.checkIsolated(time,router2);
            }
        }
    }

    /**
     * Follow an event not generated by engine
     */

    public void gcFollowEvent(SimEvent e, Object o)
    {

    }

    /**
     * Preceed an event not generated by engine
     */

    public void gcPreceedEvent(SimEvent e)
    {

    }

    /** Make sure network is connected from r1 to r2*/
    public void connectNetwork(long time,int r1, int r2)
    {
        int nRouters= routerList_.size();
        if (nRouters <=1)
            return;
        // Boolean arrays are larger than needed but this is fast
        boolean [] visited= new boolean[maxRouterId_+1];
        boolean [] visiting= new boolean[maxRouterId_+1];
        for (int i= 0; i < maxRouterId_+1; i++) {
            visited[i]= true;
            visiting[i]= false;
        }
        for (int i= 0; i < nRouters; i++) {
            visited[routerList_.get(i)]= false;
        }
        int [] toVisit= new int[nRouters];
        int toVisitCtr= 1;
        toVisit[0]= r1;
        int noVisited= 1;
        while (noVisited < nRouters) {
            //System.err.println("NoVisited = "+noVisited+" / "+nRouters);
            if (toVisitCtr == 0) { // Not visited everything so make a new link
                // Choose i1 th visited and i2 th unvisited
                int i1= (int)Math.floor(Math.random()*noVisited);
                int i2= (int)Math.floor(Math.random()*(nRouters-noVisited));
                int l1= -1;
                int l2= -1;
                //System.err.println("Pick "+i1+" from "+noVisited+" visited nodes");
                //System.err.println("Pick "+i2+" from "+(nRouters-noVisited)+" unvisited nodes");
                for (int i= 0; i < nRouters; i++) {
                    int tmpNode= routerList_.get(i);
                    if (visited[tmpNode] && i1 >= 0) {

                        if (i1 == 0) {
                            l1= tmpNode;
                        }
                        i1--;
                    } else if (!visited[tmpNode] && i2 >= 0) {

                        if (i2 == 0) {
                            l2= tmpNode;
                        }
                        i2--;
                    }
                    if (i1 < 0 && i2 < 0)
                        break;
                }
                if (l1 == -1 || l2 == -1) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Error in network connection "+l1+" "+l2);
                    bailOut();
                    return;
                }
                // Make a new link to connect network
                //System.err.println("Link "+l1+" is "+visited[l1]+" "+l2+" is "+visited[l2]);
                startLink(time,l1,l2, 1, null);
                toVisitCtr++;
                toVisit[0]= l2;
            }
            toVisitCtr--;
            int node= toVisit[toVisitCtr];
            visited[node]= true;
            noVisited++;
            for (int l : getOutLinks(node)) {
                if (l==r2)    // We have a connection
                    return;
                if (visited[l] == false && visiting[l] == false) {
                    toVisit[toVisitCtr]= l;
                    visiting[l]= true;
                    toVisitCtr++;
                }
            }
        }

    }

    public boolean isLatticeMonitoring()
    {
        return latticeMonitoring;
    }

    /** Accessor function for ControlOptions structure options_ */
    public ControlOptions getOptions() {
        return options_;
    }

    /** Accessor function for routerList */
    public ArrayList<Integer> getRouterList() {
        return routerList_;
    }

    /** Return id of ith router */
    public int getRouterId(int i) {
        return routerList_.get(i);
    }

    /** Number of routers in simulation */
    public int getNoRouters() {
        return routerList_.size();
    }


    /**
     * Get the name of this GlobalController.
     */
    public String getName() {
        if (myHostInfo_ == null) {
            return myName;
        }
        return myName + ":" + myHostInfo_.getPort();
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

