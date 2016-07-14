package usr.localcontroller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.common.LocalHostInfo;
import usr.common.ProcessWrapper;
import usr.console.ComponentController;
import usr.interactor.GlobalControllerInteractor;
import usr.interactor.RouterInteractor;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.router.RouterOptions;
import cc.clayman.console.ManagementConsole;
import java.lang.reflect.Constructor;
import eu.reservoir.monitoring.appl.BasicDataSource;
import eu.reservoir.monitoring.appl.datarate.EveryNMilliseconds;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneProducerWithNames;


/** The local controller is intended to run on every machine.
 * Its job is to start up router processes as needed.
 * It should be started by being given a port to listen on
 * specified on the command line
 * java LocalController.java 8080
 * In its own start up the Global Controller will contact each
 * Local Controller to give it more state
 */
public class LocalController implements ComponentController {
    private LocalControllerInfo hostInfo_;
    private GlobalControllerInteractor gcInteractor_ = null;
    private LocalControllerManagementConsole console_ = null;
    private ArrayList<BasicRouterInfo> routers_ = null;
    private ArrayList<RouterInteractor> routerInteractors_ = null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;
    private HashMap<Integer, BasicRouterInfo> routerMap_ = null;
    private String classPath_ = null;

    private RouterOptions routerOptions_ = null;
    private String myName = "LocalController";

    private String routerConfigString_ = "";

    // Doing Lattice monitoring ?
    private boolean latticeMonitoring = false;
    private InetSocketAddress monitoringAddress;
    private int routerMonitoringTimeout;

    // A BasicDataSource for the stats of a Router
    private BasicDataSource dataSource = null;

    // The probes
    private ArrayList<LocalControllerProbe> probeList = null;

    private HashMap<String, Integer> probeInfoMap = null;


    /**
     * Main entry point.
     */
    public static void main(String[] args) {

        final LocalController self_;

        if (args.length != 2) {
            Logger.getLogger("log").logln(USR.ERROR, "Command line must specify "+
                                          "port number to listen on and nothing else.");
            System.exit(-1);
        }

        String hostname  = null;
        int port = 0;

        try {
            hostname = args[0];
            port = Integer.parseInt(args[1]);

            if (port < 0) {
                throw new NumberFormatException ("Port number must be > 0");
            }
        } catch (NumberFormatException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Unable to understand port number."+
                                          e.getMessage());
            System.exit(-1);
        }

        self_ = new LocalController(hostname, port);

    }

    /** Constructor for local controller starting on port */
    public LocalController (String hostname, int port) {
        try {
            hostInfo_ = new LocalControllerInfo(hostname, port);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot find host info for controller");
            shutDown();
        }

        routers_ = new ArrayList<BasicRouterInfo>();
        childProcessWrappers_ = new HashMap<String, ProcessWrapper>();
        routerMap_ = new HashMap<Integer, BasicRouterInfo>();
        routerInteractors_ = new ArrayList<RouterInteractor>();
        console_ = new LocalControllerManagementConsole(this, port);
        Properties prop = System.getProperties();
        classPath_ = prop.getProperty("java.class.path", null);

        // setup DataSource
        dataSource = new BasicDataSource(getName() + ".dataSource");
        probeList = new ArrayList<LocalControllerProbe>();

        probeInfoMap = new HashMap<String, Integer>();

        // hack in a probe
        probeInfoMap.put("usr.localcontroller.HostInfoProbe", 5000);

        init();

        console_.start();

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + hostInfo_.getIp() + ":" + hostInfo_.getPort() +  " Starting.");

    }

    private void init() {
        routerOptions_ = new RouterOptions();
        // allocate a new logger
        Logger logger = Logger.getLogger("log");
        // tell it to output to stdout
        // and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.STDOUT set
        logger.addOutput(System.out, new BitMask(USR.STDOUT));
        // tell it to output to stderr
        // and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.ERROR set
        logger.addOutput(System.err, new BitMask(USR.ERROR));

        // Setup probes
        setupProbes(probeInfoMap);

    }

    /**
     * Get the name of this LocalController.
     */
    @Override
    public String getName() {
        return  hostInfo_.getName() + ":" + hostInfo_.getPort();
    }

    /** Received shut Down data gram from global */
    public void shutDown() {

        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Local controller " + getName() + " got shutdown message from global controller.");

        //ThreadTools.findAllThreads("LC top of shutDown:");

        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Stopping all running routers");
        synchronized (routerInteractors_) {

            for (int i = 0; i < routers_.size(); i++) {

                RouterInteractor interactor = routerInteractors_.get(i);
                try {
                    interactor.shutDown();
                } catch (IOException e) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot send shut down to Router");
                    Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
                } catch (JSONException e) {
                    Logger.getLogger("log").logln(USR.ERROR,
                                                  leadin() + "Cannot send shut down to Router");
                    Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
                }

                //ThreadTools.findAllThreads("LC after router shutDown:");

            }

        }

        // Stop monitoring
        if (latticeMonitoring) {
            stopMonitoring();
        }


        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping process wrappers");
        Collection<ProcessWrapper> pws = childProcessWrappers_.values();

        for (ProcessWrapper pw : pws) {
            pw.stop();
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping global controller interactor");
        try {
            gcInteractor_.quit();
        } catch (Exception e) {

            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot exit from global interactor");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping console");
        console_.stop();

    Logger.getLogger("log").logln(USR.STDOUT, leadin()+ hostInfo_.getIp() + ":" + hostInfo_.getPort() + " Stopping.");

        /*
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            // Logger.getLogger("log").logln(USR.ERROR, leadin()+ e.getMessage());

        }
        */
        //ThreadTools.findAllThreads("LC end of shutDown:");


    }

    /**
     * Get the host info the the host this is a LocalController for.
     */
    public LocalControllerInfo getHostInfo() {
        return hostInfo_;
    }

    /**
     * Send 'alive' message to GlobalController.
     */    
    public void aliveMessage(LocalHostInfo gc) {
        Logger.getLogger("log").logln(USR.STDOUT, "Got alive message from global controller.");
        try {
            Logger.getLogger("log").logln(USR.STDOUT, "Sending to "+gc.getName()+":"+gc.getPort());
            gcInteractor_ = new GlobalControllerInteractor(gc);

        } catch (UnknownHostException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot contact global controller");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            System.exit(-1);
        } catch (IOException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot contact global controller");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            System.exit(-1);
        }

    }


    /**
     * Received start new router command
     * @return the name of the router, on success, or null, on failure.
     */
    @SuppressWarnings("unused")
    public String requestNewRouter (int routerId, int port1, int port2, String address, String name) {

        String routerName;
        Address routerAddress = null;
        try {
            if (address == null || address.equals("")) {

                routerAddress = AddressFactory.newAddress(routerId);

            } else {

                routerAddress = AddressFactory.newAddress(address);
            }
        } catch (UnknownHostException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot construct address for router");
            return null;
        }

        if (name == null || name.equals("")) {
            routerName = "Router-" + routerId;
        } else {
            routerName = name;
        }

        // Setup router class
        String routerClassName = routerOptions_.getRouterClassName();

        if (routerClassName == null) {
            // there is no routerClassName defined in the options
            // use the built-in one
            routerClassName = "usr.router.Router";
        }

        

        Process child = null;
        ProcessWrapper pw = null;
        // was 9
        String [] cmd = new String[10];
        cmd[0] = "java";
        cmd[1] = "-cp";
        cmd[2] = classPath_;

        // to run more apps
        //cmd[3] = "-Xms32m";
        //cmd[4] = "-Xmx512m";
        // for better scalability (Lefteris)
        cmd[3] = "-Xms16m";  // was 32
        cmd[4] = "-Xmx64m";  // was 128

        // for better scalability (sclayman)
        cmd[5] = "-Xss256k";
        
        cmd[6] = routerClassName;
        cmd[7] = String.valueOf(port1);
        cmd[8] = String.valueOf(port2);
        cmd[9] = routerName;

        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting Router on ports "+port1+" "+port2);

            child = new ProcessBuilder(cmd).start();
            pw = new ProcessWrapper(child, routerName);


        } catch (IOException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unable to execute command "+ Arrays.asList(cmd));
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());


            if (child != null) {
                child = null;
            }

            if (pw != null) {
                pw.stop();
            }
            //System.exit(-1);
            return null;
        }

        childProcessWrappers_.put(routerName, pw);
        RouterInteractor interactor = null;

        // try 20 times, with 250 millisecond gap (max 5 seconds)
        int MAX_TRIES = 20;
        int tries = 0;
        int millis = 250;
        boolean isOK = false;

        for (tries = 0; tries < MAX_TRIES; tries++) {
            // sleep a bit
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
            }

            // try and connect
            try {
                // connect to ManagementConsole of the router on port port1
                interactor = new RouterInteractor("localhost", port1);
                isOK = interactor.routerOK();

                synchronized (routerInteractors_) {
                    routerInteractors_.add(interactor);
                }

                break;
            } catch (UnknownHostException uhe) { // Try again
            } catch (IOException e) {
            } catch (JSONException ex) {
            }
        }

        if (!isOK) {
            // we didnt connect
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unable to connect to Router on port " + port1);

            // stop process
            if (pw != null) {
                pw.stop();
            }
            child = null;
            try {
                if (interactor != null) {
                    interactor.shutDown();
                }
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "IOException connecting to Router on port " + port1);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            } catch (JSONException e2) {
            }

            return null;
        }
        BasicRouterInfo br = new BasicRouterInfo(routerId, 0, hostInfo_, port1);
        routers_.add(br);

        // tell the router its new name and config if available
        try {
            if (!"".equals(routerConfigString_)) {
                interactor.setConfigString(routerConfigString_);
            }

            interactor.setName(routerName);
            br.setName(routerName);
            interactor.setAddress(routerAddress);
            br.setAddress(routerAddress.asTransmitForm());

            // tell the router to start some monitoring
            if (latticeMonitoring) {
                interactor.monitoringStart(monitoringAddress, routerMonitoringTimeout);
            }

        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                          "IOException setting interactor details for Router on port " + port1);
            pw.stop();
            child = null;
            try {
                interactor.shutDown();
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "IOException connecting to Router on port " + port1);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            } catch (JSONException e2) {
            }

            return null;
        } catch (JSONException mcrpe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                          "MCRP Exception setting interactor details for Router on port " + port1);
            pw.stop();
            child = null;
            try {
                interactor.shutDown();
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "IOException connecting to Router on port " + port1);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            } catch (JSONException e2) {
            }

            return null;
        }

        // keep router ID -> BasicRouterInfo
        routerMap_.put(routerId, br);

        return routerName;


    }

    /**
     * Connect two Routers on two specified hosts.
     * @return the name of the connection, on success, or null, on failure.
     */
    public JSONObject connectRouters(LocalHostInfo r1, LocalHostInfo r2, int weight, String name) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Got connect request for routers " + r1 + " " + r2);

        RouterInteractor ri = findRouterInteractor(r1.getPort());

        if (ri == null) {
            return null;
        } else {
            try {
                // The destination end-point of a connection
                // which starts from r1
                String address = r2.getName()+":"+r2.getPort();

                // Create a connection
                // Exampel response {"address":"3","name":"Router-3.Connection-0","port":0,"remoteAddress":"4","remoteName":"Router-4","weight":1}
                JSONObject response = null;
                String connectionName;

                if (name == null) {
                    response = ri.createConnection(address, weight);
                    connectionName = (String)response.get("name");

                } else {
                    response = ri.createConnection(address, weight, name);
                    connectionName = (String)response.get("name");
                }

                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + "Connection from Router: " + r1 + " to Router: " + r2 + " is " +
                                              connectionName);


                return response;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, "Cannot connect routers");
                Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
                return null;
            }
        }
    }

    /** Send router stats to global controller */
    public boolean sendRouterStats(List<String> list) {
        synchronized (gcInteractor_) { // Synchronize on gcInteractor to prevent overlap
            StringBuilder sb = new StringBuilder();

            for (String s : list) {
                sb.append(s);
                sb.append("***");
            }
            String allStats = sb.toString();
            try {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "sendRouterStats " + allStats);

                gcInteractor_.sendRouterStats(allStats);
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                              "IO EXception, cannot send stats to global controller "+e.getMessage());
                Logger.getLogger("log").logln(USR.ERROR, leadin()+ "Full stats \n:"+
                                              allStats);
                return false;
            } catch (JSONException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                              "JSON Excpetion cannot send stats to global controller "+e.getMessage());
                return false;
            }

            return true;
        }
    }



    /** Local controller receives request to end a router */
    public boolean endRouter(LocalHostInfo r1) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() +
                                      "Got terminate request for router "+r1);
        RouterInteractor ri = findRouterInteractor(r1.getPort());

        if (ri == null) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Cannot find router interactor -- router assumed closed");
            return true;
        }

        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Sending terminate request via interactor");
            ri.shutDown();
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Error shutting down router");
            Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());
            return false;
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() +
                                      "Got shutdown response from router "+r1);

        synchronized (routerInteractors_) {
            int index = routerInteractors_.indexOf(ri);
            routerInteractors_.remove(index);
            int i;

            for (i = 0; i < routers_.size(); i++) {
                if (routers_.get(i).getManagementPort() == r1.getPort()) {
                    break;
                }
            }

            if (i == routers_.size()) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"Router not registered with localcontroller");
                return false;
            }
            BasicRouterInfo br = routers_.get(i);
            String name = br.getName();
            routerMap_.remove(br.getId());
            routers_.remove(i);
            ProcessWrapper p = childProcessWrappers_.get(name);
            //System.err.println("PRocess wrapper "+p+" name "+name);
            p.stop();
            //System.err.println(p.getName());
            childProcessWrappers_.remove(name);
            return true;
        }
    }

    /** Local controller receives request to end a router */
    public boolean endLink(LocalHostInfo r1, String r2Addr) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() +
                                      "Got terminate request for link from"+r1+" to "+r2Addr);
        RouterInteractor ri = findRouterInteractor(r1.getPort());

        if (ri == null) {
            return false;
        }
        try {
            ri.endLink(r2Addr);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Error endLink from " + r1+" to "+r2Addr);
            Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());
            return false;
        }

        return true;
    }

    /** Local controller receives request to set a link weight */
    public boolean setLinkWeight(LocalHostInfo r1, String r2Addr, int weight) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() +
                                      "Got request to set link weight from"+r1+" to "+r2Addr + " weight: " + weight);
        RouterInteractor ri = findRouterInteractor(r1.getPort());

        if (ri == null) {
            return false;
        }
        try {
            ri.setLinkWeight(r2Addr, weight);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Error setLinkWeight from " + r1+" to "+r2Addr + " weight: " + weight);
            Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());
            return false;
        }

        return true;
    }

    /** Set string which configures routers */
    public void setRouterOptions(String str) {
        Logger logger = Logger.getLogger("log");
        routerConfigString_ = str;
        try {
            routerOptions_.setOptionsFromString(str);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Cannot read options string");
            return;
        }

        String fileName = routerOptions_.getOutputFile();

        if (!fileName.equals("")) {
            if (routerOptions_.getOutputFileAddName()) {
                fileName += "_"+leadinFname();
            }
            File output = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output, true);
                PrintWriter pw = new PrintWriter(fos, true);
                logger.removeOutput(System.out);
                logger.addOutput(pw, new BitMask(USR.STDOUT));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.exit(-1);
            }
        }
        String errorName = routerOptions_.getErrorFile();

        if (!errorName.equals("")) {
            if (routerOptions_.getOutputFileAddName()) {
                errorName += "_"+leadinFname();
            }
            File output = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output, true);
                PrintWriter pw = new PrintWriter(fos, true);
                logger.removeOutput(System.err);
                logger.addOutput(pw, new BitMask(USR.ERROR));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.exit(-1);
            }
        }

    }

    /** Set the Aggregation point for a given router */
    public boolean setAP(int GID, int AP) {
        BasicRouterInfo br = routerMap_.get(GID);

        if (br == null) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin()+" cannot find information for router "+GID+
                                          " to set AP");
            return false;
        }
        int port = br.getManagementPort();
        RouterInteractor ri = findRouterInteractor(port);

        if (ri == null) {
            return false;
        }
        try {
            ri.setAP(GID, AP);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin()+"cannot set aggregation point for router "+GID);
            return false;
        }

        return true;
    }

    /**
     * Get router stats for all routers managed by this LocalController
     */
    public synchronized List<String> getRouterStats() {

        Set<Integer> routerIDs = routerMap_.keySet();

        List<String> result = new ArrayList<String>();

        // now add the stats for each router to the list
        for (int routerID : routerIDs) {
            Logger.getLogger("log").logln(USR.STDOUT, "getRouterStats " + routerID);

            List<String> routerStats = getRouterStats(routerID);

            if (routerStats != null) {
                result.addAll(routerStats);
            }
        }

        return result;
    }

    /**
     * Get some router stats
     */
    public synchronized List<String> getRouterStats(int routerID) {
        BasicRouterInfo br = routerMap_.get(routerID);

        if (br == null) {
            return null;
        }
        int port = br.getManagementPort();
        RouterInteractor ri = findRouterInteractor(port);

        if (ri == null) {
            return null;
        } else {
            try {
                // original data

                List<String> list = ri.getNetIFStats();
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"requesting statistics from router "+routerID);

                // now add the router id to each element of the list
                List<String> newList = new ArrayList<String>();

                for (String stat : list) {
                    newList.add(routerID + " " + stat);
                }

                return newList;

            } catch (JSONException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"JSON error in getRouterStats()"+" type "+
                                              e.getClass().getName() + "Message:"+e.getMessage());
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"IOError in getRouterStats()"+" type "+
                                              e.getClass().getName() + "Message:"+e.getMessage());
                return null;
            }

        }
    }


    /**
     * Run something on a Router.
     */
    public JSONObject onRouter(int routerID, String className, String[] args) {
        BasicRouterInfo br = routerMap_.get(routerID);

        int port = br.getManagementPort();
        RouterInteractor ri = findRouterInteractor(port);

        if (ri == null) {
            return null;
        } else {
            try {

                JSONObject jsonObj = ri.appStart(className, args);

                return jsonObj;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"");
                return null;
            }

        }

    }

    /**
     * Stop something on a Router.
     */
    public JSONObject appStop(int routerID, String appName) {
        BasicRouterInfo br = routerMap_.get(routerID);

        int port = br.getManagementPort();
        RouterInteractor ri = findRouterInteractor(port);

        if (ri == null) {
            return null;
        } else {
            try {

                JSONObject jsonObj = ri.appStop(appName);

                return jsonObj;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"");
                return null;
            }

        }

    }

    /** Report the Aggregation point for a given router */
    public boolean reportAP(int GID, int AP) {
        try {
            gcInteractor_.reportAP(GID, AP);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin()+"cannot set aggregation point for router "+GID);
            return false;
        }

        return true;
    }

    /**
     * Find some router info
     */
    public BasicRouterInfo findRouterInfo(int rId) {
        return routerMap_.get(rId);
    }

    /**
     * Get a BasicRouterInfo by looking up a router port.
     */
    public BasicRouterInfo findRouterInfoByPort(int port) {
        for (BasicRouterInfo bri : routers_) {
            if (port == bri.getManagementPort()) {
                return bri;
            }
        }
        Logger.getLogger("log").logln(USR.ERROR,
                                      leadin()+"Unable to find BasicRouterInfo listening on port "+port);
        return null;
    }

    /**
     * Get the ManagementConsole this ComponentController interacts with.
     */
    @Override
    public ManagementConsole getManagementConsole() {
        return console_;
    }

    /**
     * Get a RouterInteractor by looking up a router port.
     */
    private RouterInteractor findRouterInteractor(int port) {
        for (RouterInteractor r : routerInteractors_) {
            if (port == r.getPort()) {
                return r;
            }
        }
        Logger.getLogger("log").logln(USR.ERROR,
                                      leadin()+"Unable to find router interactor listening on port "+port);
        return null;
    }

    public GlobalControllerInteractor getGlobalControllerInteractor() {
        return gcInteractor_;
    }

    /***  MONITORING  ***/

    /**
     * Setup monitoring, sending data to a specified address
     * with a particular timeout for routers.
     */
    private void setUpRouterMonitoring(InetSocketAddress socketAddress, int timeout) {
        latticeMonitoring = true;
        monitoringAddress = socketAddress;
        routerMonitoringTimeout = timeout;
    }

    /**
     * Stop any monitoring on Routers
     */
    private void stopRouterMonitoring() {
        // tell all the Routers to stop monitoring
        if (latticeMonitoring) {
            for (int i = 0; i < routers_.size(); i++) {

                RouterInteractor interactor = routerInteractors_.get(i);
                try {
                    interactor.monitoringStop();
                } catch (IOException e) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot send stopMonitoring to Router");
                    Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
                } catch (JSONException e) {
                    Logger.getLogger("log").logln(USR.ERROR,
                                                  leadin() + "Cannot send stopMonitoring to Router");
                    Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
                }
            }
            latticeMonitoring = false;
        }
    }


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
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "startMonitoring to " + addr);

        DataPlane outputDataPlane = new UDPDataPlaneProducerWithNames(addr);
        dataSource.setDataPlane(outputDataPlane);

        // add probes
        for (LocalControllerProbe probe : probeList) {
            dataSource.addProbe(probe);  // this does registerProbe and activateProbe
        }

        // and connect
        dataSource.connect();

        // turn on probes
        for (LocalControllerProbe probe : probeList) {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          leadin() + " data source: " +  dataSource.getName() + " turn on probe " +
                                          probe.getName());

            dataSource.turnOnProbe(probe);

            probe.started();
        }

        setUpRouterMonitoring(addr, when);

    }

    /**
     * Pause monitoring
     */
    public synchronized void pauseMonitoring() {
        for (LocalControllerProbe probe : probeList) {
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
        for (LocalControllerProbe probe : probeList) {
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
        stopRouterMonitoring();

        if (dataSource.isConnected()) {
            //pauseMonitoring();

            // turn off probes
            for (LocalControllerProbe probe : probeList) {
                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + " data source: " +  dataSource.getName() + " turn off probe " +
                                              probe.getName());

                probe.lastMeasurement();

                dataSource.turnOffProbe(probe);

            }

            // disconnect
            dataSource.disconnect();

            // remove probes
            for (LocalControllerProbe probe : probeList) {
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
                Class<? extends LocalControllerProbe> cc = c.asSubclass(LocalControllerProbe.class );

                // find Constructor for when arg is RouterController
                Constructor<? extends LocalControllerProbe> cons = cc.getDeclaredConstructor(LocalController.class);

                LocalControllerProbe probe = cons.newInstance(this);

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
     * Create the String to append to file names
     */
    String leadinFname() {
        return "LC_"+ hostInfo_.getName() + "_" + hostInfo_.getPort();
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String LC = "LC: ";

        return getName() + " " + LC;
    }

}
