package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import usr.logging.*;
import usr.console.*;
import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import usr.common.*;
import java.util.concurrent.*;
import usr.interactor.*;
import usr.APcontroller.*;
import usr.router.RouterOptions;
import usr.output.OutputType;
import java.nio.channels.FileChannel;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneConsumerWithNames;
import eu.reservoir.monitoring.appl.BasicConsumer;
import java.net.InetSocketAddress;

/**
 * The GlobalController is in overall control of the software.  It
 * contacts LocalControllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
public class GlobalController implements ComponentController {
    private long simulationTime;   // Current time in simulation
    private long simulationStartTime;  // time at which simulation started  (assuming realtime)
    private long lastEventLength;   // length of time previous event took
    private String xmlFile_;          // name of XML file containing config
    private LocalHostInfo myHostInfo_;  // Information about the local info 
    private ControlOptions options_;   // Options affecting the simulation
    private boolean listening_;  			// Is this
    private GlobalControllerManagementConsole console_= null;
    private ArrayList <LocalControllerInteractor> localControllers_ = null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;
    private ArrayList <String> childNames_= null;  // names of child processes

    
    // Arrays below are sparse and this approach is for fast access.
    // outLinks_.get(i) returns a primitive array containing all the
    // nodes directly connected from node i
    private ArrayList <int []> outLinks_= null;  // numbers of nodes which are connected from 
												// a given node 
    private ArrayList <int []> linkCosts_= null;	// costs of connections in above
    private ArrayList <Integer> routerList_= null;   // List of integers which
										// contains the numbers of nodes present

    private HashMap<Pair<Integer,Integer>, String> linkNames = null; // A map of (routerID, routerID) links to link names
				
    private HashMap <LocalControllerInfo, LocalControllerInteractor> interactorMap_= null;
		// Map connections LocalControllerInfo for given LCs to the appropriate interactors

    private HashMap <Integer, BasicRouterInfo> routerIdMap_= null;
    // Map is from router Id to information one which machine router is stored on.
    
    private HashMap <LocalControllerInfo, PortPool> portPools_= null;
    // Map is used to store vacant ports on local controllers
    
    private int aliveCount= 0;
    // Counts number of live nodes running.
    
    private EventScheduler scheduler_= null;
    // Class holds scheduler for event list
    
    private boolean simulationRunning_= true;
    // Used to stop simulation
    
    private int maxRouterId_=0;
    // Maximum ID no of any router instantiated so far.  Next router
    // will have number maxRouterId_+1
    
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
    // and a Reporter object that handles the incoming measurements
    NetIFStatsReporter reporter;


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

      outLinks_= new ArrayList<int []> ();
      linkCosts_= new ArrayList<int []> ();
      routerList_= new ArrayList<Integer>();
      linkNames = new HashMap<Pair<Integer,Integer>, String>();
      options_= new ControlOptions(xmlFile_);
      routerOptions_= options_.getRouterOptions();
      waitCounter_= new Object();

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
      APController_= ConstructAPController.constructAPController
        (routerOptions_);
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

          // setup DataConsumer
          dataConsumer = new BasicConsumer();
          // and reporter
          reporter = new NetIFStatsReporter(this);

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
      for (OutputType o: options_.getOutputs()) {
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
                long eventTime= e.getTime();
                simulationTime= System.currentTimeMillis();
                
                if (simulationTime - (simulationStartTime + eventTime) > 
                     options_.getMaxLag()) {
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
    
    /** Check queued messages at Global Controller */
    private boolean checkMessages()
    {
        BlockingQueue<Request> queue = console_.queue();
        if (queue.size() == 0)
           return false;
        Request req= queue.remove();
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "TODO -- need to deal with event here!");
        return true;
    }
    
    

     /** bail out of simulation relatively gracefully */
    private void bailOut() {
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Bailing out of simulation!");
        shutDown();
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Exit after bailout");
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Bailing out of simulation!");
    }
    
    private void executeEvent(SimEvent e) {
        Object extraParms= null;
        long eventBegin = System.currentTimeMillis();
        options_.preceedEvent(e,scheduler_,this);
        Logger.getLogger("log").logln(USR.STDOUT, "SIMULATION: " + "<" + lastEventLength + "> " +
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
                
                startRouter(time);
            }
            else if (type == SimEvent.EVENT_END_ROUTER) {
                int routerNo= (Integer)e.getData();
                endRouter(time,routerNo);
            }
            else if (type == SimEvent.EVENT_START_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                startLink(time,router1, router2);
            }
            else if (type == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                endLink(time,router1, router2);
            }
            else if (type == SimEvent.EVENT_AP_CONTROLLER) {
                queryAPController(time);
            }
            else if (type == SimEvent.EVENT_OUTPUT) {
                produceOutput(time,(OutputType)(e.getData()));
            } 
            else if (type == SimEvent.EVENT_ON_ROUTER) {
                String[] eventArgs = (String[])e.getData();
                Scanner sc = new Scanner(eventArgs[0]);

                int routerID = sc.nextInt();
                String className = eventArgs[1];

                // eliminate router_id and className
                String[] cmdArgs = new String[eventArgs.length-2];

                for (int a=2; a < eventArgs.length; a++) {
                    cmdArgs[a-2] = eventArgs[a];
                }
                
                // call onRouter with correct args
                onRouter(routerID, className, cmdArgs);

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
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Event "+type+" had wrong object");
            shutDown();
            return;
        }
        options_.followEvent(e,scheduler_,this, extraParms);

    }
    
    /** Event for start Simulation */
    private void startSimulation(long time) {
        
        lastEventLength = 0;
         Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Start of simulation event at: " +  
          time+ " "+System.currentTimeMillis());    
        for (OutputType o: options_.getOutputs()) {
            if (o.getTimeType() == OutputType.AT_START) {
                produceOutput(time,o);
            }
        }
    }
    
    /** Event for end Simulation */
    private void endSimulation(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "End of simulation event at " + 
          time+ " "+System.currentTimeMillis());
        simulationRunning_= false;
        
        for (OutputType o: options_.getOutputs()) {
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
    private void startRouter(long time) {
        maxRouterId_++;
        int rId= maxRouterId_;
        outLinks_.add(new int [0]);
        linkCosts_.add(new int [0]);
        registerRouter(rId);
        APController_.addNode(time, rId);
        if (!options_.isSimulation()) {
            //System.err.println("Trying to start");
            if (startVirtualRouter(maxRouterId_) == false) {
              //  System.err.println("Did not start");
                unregisterRouter(rId);
            }
        }
    }
    
    private boolean startVirtualRouter(int id) 
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
                if (tryRouterStart(id, leastUsed, lci)) { 
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
    boolean tryRouterStart (int id, LocalControllerInfo local, LocalControllerInteractor lci) 
        throws IOException {
        int port= 0;
        PortPool pp= portPools_.get(local);
        String routerName;
        try {
            port = pp.findPort(2);
            
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Creating router " + id);
            // create the new router and get it's name
            routerName = lci.newRouter(id, port);
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Created router " + routerName);
        }
        catch (MCRPException e) { 
           // Failed to start#
           Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not create router " + id + " on "+lci);
           if (port != 0) 
              pp.freePorts(port,port+1);  // Free ports but different ones will be tried next time
           return false;
        }
        
        BasicRouterInfo br= new BasicRouterInfo(id,simulationTime,local,port);
        br.setName(routerName);
        // keep a handle on this router
        routerIdMap_.put(id,br);
        return true;
        
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
            if (reporter == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+ 
                "Freporter does not exist shutting down" +
                br.getHost()+":"+br.getManagementPort());
                return;
            }
            reporter.routerDeleted(routerName);
        }
    }
    
    /** Event to end a router */
    private void endRouter(long time, int routerId) {
        
        if (options_.isSimulation()) {
            endSimulationRouter(routerId);
        } else {
            endVirtualRouter(routerId);
        }
        unregisterRouter(routerId);
        APController_.removeNode(time, routerId);
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
    private void startLink(long time, int router1Id, int router2Id) {
        //Logger.getLogger("log").logln(USR.ERROR, "Start link "+router1Id+" "+router2Id);
        
        int index= routerList_.indexOf(router1Id);
        if (index == -1)
            return;  // Cannot start link as router 1 dead already
        index= routerList_.indexOf(router2Id);
        if (index == -1)
            return;  // Cannot start link as router 2 dead already
        // check if this link already exists
        int [] outForRouter1 = getOutLinks(router1Id);
        
        boolean gotIt= false;
        for (int i: outForRouter1) {
            if (i == router2Id) {
                gotIt= true;
                break;
            }
        }
        if (gotIt) {
            // we already have this link
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Link already exists: "+router1Id + " -> " + router2Id);
        } else {
            if (options_.isSimulation()) {
                startSimulationLink(router1Id, router2Id);       
            } else {
                startVirtualLink(router1Id, router2Id);
            }
            registerLink(router1Id, router2Id);
            APController_.addLink(time, router1Id,router2Id);
        }
    }
    
    /** Start simulation link */
    private void startSimulationLink(int router1Id, int router2Id) {
    
    }
        
    /** Send commands to start virtual link */
    private void startVirtualLink(int router1Id, int router2Id) {
        
        BasicRouterInfo br1,br2;
        LocalControllerInfo lc;
        LocalControllerInteractor lci;
        
        
        br1= routerIdMap_.get(router1Id);
        br2= routerIdMap_.get(router2Id);
        if (br1 == null) {
            System.err.println ("Router "+router1Id+" does not exist when trying to link to "+ router2Id);
            return;
        }
        if (br2 == null) {
            System.err.println ("Router "+router2Id+" does not exist when trying to link to "+ router1Id);
            return;
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
        for (i=0; i < MAX_TRIES; i++) {
            try {
                String connectionName = lci.connectRouters(br1.getHost(), br1.getManagementPort(),
               br2.getHost(), br2.getManagementPort());

                // add Pair<router1Id, router2Id> -> connectionName to linkNames
                linkNames.put(makePair(router1Id, router2Id), connectionName);
               
               Logger.getLogger("log").logln(USR.STDOUT, leadin() + br1 + " -> " + br2 + " = " + connectionName);
               break;
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                    "Cannot link routers "+router1Id+" "+router2Id+" try "+(i+1));
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
                
            }
           catch (MCRPException e) {
              Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                    "Cannot link routers "+router1Id+" "+router2Id+" try "+(i+1));
            Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            }
        }
        if (i == MAX_TRIES) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Giving up on linking");
            bailOut();
        }   

    }
    
    /* Return a list of outlinks from a router */
    public int [] getOutLinks(int routerId)
    {
        return outLinks_.get(routerId-1);
    }
    
     /* Return a list of outlinks from a router */
    public void setOutLinks(int routerId, int []out)
    {
        outLinks_.set(routerId-1,out);
        //System.err.print(routerId+" contains ");
        //for (int i:out) 
        //    System.err.print(i+" ");
        //System.err.println();
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
    public void setLinkCosts(int routerId, int []costs)
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
        return  new Pair<Integer,Integer>(r1,r2);
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
            lci.endLink(br1.getHost(),br1.getManagementPort(),rId2);
       
            // remove Pair<router1Id, router2Id> -> connectionName to linkNames
            linkNames.remove(makePair(rId1, rId2));
               

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
    private void endLink (long time, int router1Id, int router2Id) {
        
        unregisterLink(router1Id, router2Id);
        APController_.removeLink(time, router1Id,router2Id);
        if (options_.isSimulation()) {
            endSimulationLink(router1Id, router2Id);
        } else {
            endVirtualLink(router1Id, router2Id);
        }
    }
    
    /**
     * Run something on a Router.
     */
    public String onRouter(int routerID, String className, String[] args) {
        BasicRouterInfo br = routerIdMap_.get(routerID);

        LocalControllerInteractor lci= interactorMap_.get(br.getLocalControllerInfo());

        if (lci == null) {
            return null;
        }
        int i;
        int MAX_TRIES= 5;
        for (i=0; i < MAX_TRIES; i++) {
            try {
                String appName = lci.onRouter(routerID, className, args);
                return appName;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                  " failed to start app "+className+" on "+ routerID+ " try "+i);
                  
            }
        }
        Logger.getLogger("log").logln(USR.ERROR, leadin()+
                  " failed to start app "+className+" on "+ routerID+ " giving up ");
        return null;

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
        } catch (MCRPException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        }    
    }

    /**
     * Get the router stats -- method is blocking
     */
    public List<String> getRouterStats()  {
       
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
        } catch (MCRPException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Could not get stats");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return null;
        }
      
    }

    /** Shutdown called from console -- add shut down command to list to
    happen now */
    public void shutDownCommand() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Shut down called from console");
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION,0,null);
        scheduler_.addEvent(e);
        wakeWait();
    }  
      
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
            time+routerOptions_.getControllerConsiderTime(), null);
        scheduler_.addEvent(e);
        APController_.controllerUpdate(time, this);
        
    }
    
    /** Produce some output */
    private void produceOutput(long time, OutputType o) {
        int type= o.getType();
       
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
        
        if (type == OutputType.OUTPUT_NETWORK) {
            outputNetwork(time,p,o);
        } else if (type == OutputType.OUTPUT_SUMMARY) {
            outputSummary(p,o,time);
        } else if (type == OutputType.OUTPUT_TRAFFIC) {
            outputTraffic(o,time);
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown output type "+ 
               type);
        } 
        
        // Schedule next output time
        if (o.getTimeType() == OutputType.AT_INTERVAL) {
            SimEvent e= new SimEvent(SimEvent.EVENT_OUTPUT,
                time+o.getTime(),o);
            scheduler_.addEvent(e);
        }
        p.close();
        try {
            s.close();
        } catch (java.io.IOException ex) {
        
        }
    }
    
    /** Output a network */
    private void outputNetwork(long time, PrintStream s, OutputType o) {
        //System.err.println("APS are "+APController_.getAPList());
        APController_.controllerUpdate(time,this);

        boolean printAP= o.getParameter().equals("AP");
        boolean printScore= o.getParameter().equals("Score");

        if (printAP) {
            networkWithCostGraphviz(time, s);
            return;
        }

        if (printScore) {
            networkWithScoreGraphviz(time,s);
            return;
        }

        plainNetworkGraphviz(time, s);
    }

    /**
     * Send a network graph showing various attributes to a PrintStream.
     */
    public void outputNetworkUsingStyle(String style, long time, PrintStream s) {
        if (!latticeMonitoring) {
            s.close();
            return;
        }

        if (style.equals("cost") || style.equals("AP")) {
            networkWithCostGraphviz(time, s);

        } else if (style.equals("score")) {
            networkWithScoreGraphviz(time, s);

        } else if (style.equals("dot")) {
            networkGraphAsGraphvis(time, s);

        } else {
            networkGraphAsGraphvis(time, s);
        }
    }

    /**
     * Send a network showing plain network as Graphviz to a PrintStream
     */
    private void plainNetworkGraphviz(long time, PrintStream s) {
        s.println("Graph G {");
        for (int r: getRouterList()) {
            int ap= APController_.getAP(r);
            if (ap == r) {
                s.print(r+" [shape=box");
            } else {
                s.print(r+" [shape=circle");
            }

            s.println("];");
        }
        
        for (int i: getRouterList()) {
            for (int j: getOutLinks(i)) {
                if (i < j) 
                    s.println(i+ " -- "+j+";");
            }
        }
        s.println("}");
       
    }

    /**
     * Send a network showing AP costs as Graphviz to a PrintStream
     */
    private void networkWithCostGraphviz(long time, PrintStream s) {
        s.println("Graph G {");
        for (int r: getRouterList()) {
            int ap= APController_.getAP(r);
            if (ap == r) {
                s.print(r+" [shape=box");
            } else {
                s.print(r+" [shape=circle");
            }

            s.print(",label=\""+ap+" ("+APController_.getAPCost(r)+")\"");

            s.println("];");
        }
        
        for (int i: getRouterList()) {
            for (int j: getOutLinks(i)) {
                if (i < j) 
                    s.println(i+ " -- "+j+";");
            }
        }
        s.println("}");
       
    }
    
    /**
     * Send a network showing AP costs as Graphviz to a PrintStream
     */
    private void networkWithScoreGraphviz(long time, PrintStream s) {
        s.println("Graph G {");
        for (int r: getRouterList()) {
            int ap= APController_.getAP(r);
            if (ap == r) {
                s.print(r+" [shape=box");
            } else {
                s.print(r+" [shape=circle");
            }

            s.print(",label=\""+ap+" ("+APController_.getScore(time,r,this)+")\"");

            s.println("];");
        }
        
        for (int i: getRouterList()) {
            for (int j: getOutLinks(i)) {
                if (i < j) 
                    s.println(i+ " -- "+j+";");
            }
        }
        s.println("}");
       
    }
    
    /**
     * Send a Graphviz network to a PrintStream
     */
    private void networkGraphAsGraphvis(long time, PrintStream s) {
        if (!latticeMonitoring) {
            s.close();
            return;
        }

        HashMap<String, ArrayList<BasicRouterInfo>>routerLocations = new HashMap<String, ArrayList<BasicRouterInfo>>();

        // work out which router is where
        for (BasicRouterInfo routerInfo : routerIdMap_.values()) {
            String host = routerInfo.getHost();

            if (routerLocations.containsKey(host)) { // we've seen this host
                ArrayList<BasicRouterInfo> list = routerLocations.get(host);
                list.add(routerInfo);
            } else {                                 //  it's a new host
                ArrayList<BasicRouterInfo> list = new ArrayList<BasicRouterInfo>();
                list.add(routerInfo);

                routerLocations.put(host, list);
            }
        }

        // now visit each host and output the routers
        s.println("graph gg {");

        s.println("    K=1;");
        s.println("    ratio=0.7;");
        s.println("    maxiter=2;");
        s.println("    labelloc=t;");
        //s.println("    rank=source;");

        // set root node, if using twopi
        int noAPs = APController_.getNoAPs();
        int noRouters = routerIdMap_.values().size();

        if (noAPs > 0) {
            int first = APController_.getAPList().get(0);
            s.println("    root=" + first +";");
        }

        // set attributes for subgraphs
        s.println("    graph [");
        s.println("      splines=true,");
        s.println("      rankdir = \"TB\",");
        //s.println("      ranksep = 1.2,");
        s.println("      style=\"setlinewidth(2)\",");
        s.println("      center=true,");
        s.println("      overlap=false,");
        s.println("      fontname=\"Helvetica\", fontsize=16, fontcolor=red");
        s.println("    ];");

        // set attributes for nodes
        s.println("    node [style=filled, fillcolor=\"white\", fontname=\"Helvetica\"];");

        // set attributes for edges
        s.println("    edge [ fontname=\"Helvetica\", fontsize=12 ];");

        // the label of the graph
        s.print("    label=" + "\"snapshot:");
        s.print(" time=");
        long t = simulationTime-simulationStartTime;
        int totalSecs = (int)t / 1000;
        int millis = (int)t % 1000;
        int hundreths = millis / 10;
        int minutes = totalSecs / 60;
        int secs = totalSecs % 60;
        s.printf("%02d:%02d:%02d", minutes, secs, hundreths);
        s.print(" hosts=" + routerLocations.keySet().size());
        s.print(" routers=" + noRouters);
        s.print(" links=" + noLinks_);
        s.println("\";");

        // visit each host
        for (String host : routerLocations.keySet()) {
            List<BasicRouterInfo> routersOnHost = routerLocations.get(host);

            s.println("    subgraph cluster_" + host + " {");
            s.print("\tlabel=\"" + host + " routers=" + routersOnHost.size() +"\";");
            s.println("\tgraph [fontname=\"Helvetica\",fontsize=16,fontcolor=red,style=filled,fillcolor=\"0.0, 0.0, 0.97\"];");
            s.println("\tnode [ shape=ellipse, style=rounded, nodesep=2.0 ];");

            // now get routers for this host
            for (BasicRouterInfo routerInfo : routersOnHost) {
                // get a router
                int r = routerInfo.getId();

                // get the AggPoint for this router
                int ap= APController_.getAP(r);

                // get position of AggPoint in AggPoint list
                int position = APController_.getAPList().indexOf(ap);

                // work out the hue for the colour of the router
                float hue = 1.0f;

                if (position == -1) {
                    hue = 1.0f;
                } else if (position % 2 == 0) { // even
                    hue = ((float)position / 2) + 1;
                } else {
                    hue = (((float)position -1) / 2) + 5 + 1;
                }

                hue = hue / 10;

                // output the router
                
                if (ap == r) { // router is also an Agg point
                    float sat = 0.6f;
                    float value = 0.6f;

                    s.print("\t" + r +" [ shape=diamond, label=\"" + routerInfo.getName() + "\""); 
                    s.print(", style=\"filled,rounded\"" + ", fillcolor=\"" + hue + "," + sat + "," + value + "\"");  // h,s,v

                } else {  // router is not an Agg point
                    s.print("\t" + r +" [ label=\"" + routerInfo.getName() + "\"");
                    
                    if (ap == 0) {                    // router has NO nominated AggPoint
                        float huew = 0f;
                        float sat = 0f;
                        float value = 1f;

                        s.print(", style=filled, " + " fillcolor=\"" + huew + "," + sat + "," + value + "\"");  // h,s,v

                    } else {                      // router has a nominated AggPoint

                        float sat = 0f;
                        float value = 0f;

                        // is the router directly connected to its AggPoint
                        if (isConnected(r, ap)) {
                            value = 0.85f;
                            sat = 0.5f;
                        } else {
                            value = 0.95f;
                            sat = 0.2f;
                        }


                        s.print(", style=filled, " + " fillcolor=\"" + hue + "," + sat + "," + value + "\"");  // h,s,v
                    }
                }

                s.println(" ];");
            }

            s.println("    }");
        }
        
        // visit all the edges
        for (int i: getRouterList()) {
            for (int j: getOutLinks(i)) {
                if (i < j)  {
                    s.print(i+ " -- "+j);


                    s.print(" [ ");

                    String router1Name = routerIdMap_.get(i).getName();
                    String router2Name = routerIdMap_.get(j).getName();

                    /*
                    // get trafffic for link i -> j as router1Name => router2Name
                    List<Object> iToj = reporter.getTraffic(router1Name, router2Name);

                    // get trafffic for link j -> i as router2Name => router1Name
                    List<Object> jToi = reporter.getTraffic(router2Name, router1Name);

                    String name = linkNames.get(makePair(i,j));

                    //s.print("label = \"" + name + "\", ");

                    if (iToj != null) {
                        s.print(" headlabel = \"" + iToj.get(2) + "/" + iToj.get(8) + "\", ");
                    }

                    if (jToi != null) {
                        s.print(" taillabel = \"" + jToi.get(2) + "/" + jToi.get(8) + "\", ");
                    }
                    */

                    // get trafffic for link i -> j as router1Name => router2Name
                    List<Object> iToj = reporter.getTraffic(router1Name, router2Name);

                    if (iToj != null) {
                        int traffic = (Integer)iToj.get(1) + (Integer)iToj.get(7);

                        s.print("label = \"" + traffic + "\", ");

                        // link colour
                        s.print("color = \"");
                    
                        if (traffic < 1000) {
                            s.print("black");
                        } else if (traffic >= 1000 && traffic < 3000) {
                            s.print("blue");
                        } else if (traffic >= 3000 && traffic < 5000) {
                            s.print("green");
                        } else if (traffic >= 5000 && traffic < 7000) {
                            s.print("yellow");
                        } else if (traffic >= 7000 && traffic < 10000) {
                            s.print("orange");
                        } else if (traffic >= 10000) {
                            s.print("red");

                        }

                        s.print("\", ");

                        if (traffic >= 3000 && traffic < 7000) {
                            s.print(" style=\"setlinewidth(2)\", ");
                        } else if (traffic >= 7000 && traffic < 20000) {
                            s.print(" style=\"setlinewidth(3)\", ");
                        } else if (traffic >= 20000) {
                            s.print(" style=\"setlinewidth(4)\", ");
                        } else {
                        }
                    }

                    s.println(" ];");
                }
            }
        }
        s.println("}");
       
    }
    
     /** Output a network */
    private void outputSummary(PrintStream s, OutputType o, long time) {
        
        if (o.isFirst()) {
            
             s.println("#No_nodes no_links no_aps tot_ap_dist mean_life mean_AP_life");
             o.setFirst(false);
        }
        s.print(getNoRouters()+" "+noLinks_+" "+
            APController_.getNoAPs());
        APController_.controllerUpdate(time, this);
        s.print(" "+APController_.APTrafficEstimate(this));
        s.print(" "+APController_.meanNodeLife()+" "+APController_.meanAPLife());
        s.print(" "+APController_.meanAPLifeSoFar(time));
        s.println();    
    }

    /** Output traffic from the network -- this merely triggers a request rahter
    than printing */
    private  void outputTraffic(OutputType o, long time) {
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
      synchronized(routerStats_) {
        statsCount_++;
        
        routerStats_= routerStats_.concat(stats);
       // System.err.println("Stat count is "+statsCount_);
        if (statsCount_ < localControllers_.size())   // Not got all stats yet
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
            outputTraffic(o , trafficOutputTime_.get(i),p);
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
        } catch (java.io.IOException ex) {
          
        }
      }
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
        dataConsumer.setReporter(reporter);

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
            dataConsumer.setReporter(null);
        
            dataConsumer.disconnect();
        }

    }


    /** Output traffic */
    void outputTraffic (OutputType o, long t, PrintStream p) {
      synchronized(routerStats_) {
         if (routerStats_.equals(""))
            return;
         if (o.getParameter().equals("Local")) {
             outputTrafficLocal(o,t,p);
         } else if (o.getParameter().equals("Aggregate")) {
             outputTrafficAggregate(o,t,p);
         } else if (o.getParameter().equals("Raw")) {
             for (String s: routerStats_.split("\\*\\*\\*")) {
                p.println(t+" "+s);
             } 
         } else if (o.getParameter().equals("Separate")) {
             outputTrafficSeparate(o,t,p); 
         } else {
            Logger.getLogger("log").logln(USR.ERROR, "Unable to parse traffic output parameter "+o.getParameter());            
         }
       }
    }

    void outputTrafficLocal(OutputType o, long t, PrintStream p) 
    {
        for (String s: routerStats_.split("\\*\\*\\*")) {
            String []args= s.split("\\s+");
            if (o.isFirst()) {
                o.setFirst(false);
                p.print("Time r_no name ");
                for (int i= 2; i < args.length;i++) {
                    p.print(args[i].split("=")[0]);
                    p.print(" ");
                }
                p.println();
            }
            if (args.length < 2)
                continue;
            if (!args[1].equals("localnet"))
                continue;
            p.print(t+" ");
            p.print(args[0]+" "+args[1]);
            for (int i= 2; i < args.length;i++) {
                p.print(args[i].split("=")[1]);
                p.print(" ");
            }
            p.println();
        }
    }
    
    void  outputTrafficAggregate (OutputType o, long t, PrintStream p) 
    {
     
        Hashtable<Integer,Boolean> routerCount= new Hashtable<Integer, Boolean>();
        
        String []out= routerStats_.split("\\*\\*\\*");
        if (out.length < 1) 
            return;
        int nField= out[0].split("\\s+").length - 3;
        if (nField <= 0) {
           Logger.getLogger("log").logln(USR.ERROR, "Can't parse no of fields in stats line "+out[0]);
           Logger.getLogger("log").logln(USR.ERROR, "Stats Line \""+routerStats_+"\"");

           return;
        }
        
        if (trafficLinkCounts_ == null) {
            trafficLinkCounts_ = new HashMap<String, int []>();
        }
        int nLinks= 0;
        int nRouters= 0;
        int []totCount= new int [nField];
        for (int i=0; i < nField; i++) {
            totCount[i]= 0;
        }
        for (String s: out) {
            int []count= new int [nField];
            for (int i= 0; i < nField; i++) {
                count[i]= 0;
            } 
            String []args= s.split("\\s+");
            if (o.isFirst()) {
                o.setFirst(false);
                p.print("Time nRouters nLinks*2 ");
                for (int i= 3; i < args.length;i++) {
                    p.print(args[i].split("=")[0]);
                    p.print(" ");
                }
                p.println();
            }
            if (args.length < 3)
                continue;
            if (args[2].equals("localnet"))
                continue;
            nLinks++;
            
            int router= Integer.parseInt(args[0]);
            
            if (routerCount.get(router) == null) {
                nRouters++;
                routerCount.put(router,true);
                //System.err.println("Time "+t+" found router "+router);
            }   
            
            
            String linkName= args[0]+args[2];
            
            for (int i= 3; i < args.length;i++) {
                String[] spl= args[i].split("=");
                if (spl.length !=2) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin()+
                      " Cannot parse traffic stats "+args[i]);
                } else {
                   count[i-3]=Integer.parseInt(spl[1]);
                
                }
            }
            //System.err.println("new count "+linkName+" "+count[0]);
            int []oldCount= trafficLinkCounts_.get(linkName);
            if (oldCount == null) {
                
                for (int i= 0; i < nField; i++) {
                    totCount[i]+= count[i];
                }
            } else {
                //System.err.println("old count "+linkName+" "+oldCount[0]);
                for (int i= 0; i < nField; i++) {
                    totCount[i]+= count[i]-oldCount[i];
                    
                }
            }
            trafficLinkCounts_.put(linkName,count);
        }
        
 
        p.print(t+" "+nRouters+" "+nLinks+" ");
        for (int i= 0; i < nField; i++) {
            p.print(totCount[i]+" ");
        }
        p.println();
    }
    
    void outputTrafficSeparate (OutputType o, long t, PrintStream p) 
    {   
        //System.err.println("Performing output");
        String []out= routerStats_.split("\\*\\*\\*");
        if (out.length < 1) 
            return;
        for (String s: out) { 
            //System.err.println("String is "+s);
            String []args= s.split("\\s+");
            if (o.isFirst()) {
                o.setFirst(false);
                p.print("Time r_no name ");
                for (int i= 2; i < args.length;i++) {
                    p.print(args[i].split("=")[0]);
                    p.print(" ");
                }
                p.println();
            }
            if (args.length < 2)
                continue;
            if (args[1].equals("localnet"))
                continue;
            p.print(t+" ");
            p.print(args[1]+" "+args[2] + " ");
            for (int i= 3; i < args.length;i++) {
                String []splitit= args[i].split("=");
                if (splitit.length < 2) {
                    System.err.println("Cannot split "+ args[i]);
                } else {
                    p.print(args[i].split("=")[1]);
                    p.print(" ");
                }
            }
            p.println();
        }
    }
    
    /** Output summary */
    
    private void initSchedule() {
        scheduler_= new EventScheduler();
        options_.initialEvents(scheduler_,this);
        
    }
    
    
    private void startLocalControllers() {
        Iterator i= options_.getControllersIterator();
        Process child= null;

        while (i.hasNext()) {
            
            LocalControllerInfo lh= (LocalControllerInfo)i.next();
            String []cmd= options_.localControllerStartCommand(lh);
            try {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting process " + Arrays.asList(cmd));
                child = new ProcessBuilder(cmd).start();
            } catch (java.io.IOException e) {
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
     * Get the ManagementConsole this ComponentController interacts with.
     */
    public ManagementConsole getManagementConsole() {
        return console_;
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

            Logger.getLogger("log").logln(USR.STDOUT, "SIMULATION: " +  "<" + lastEventLength + "> " +
                               (now - simulationStartTime) + " @ " + 
                               now +  " waiting " + timeout);
            synchronized(waitCounter_) {
                waitCounter_.wait(timeout);
            }
            lastEventLength = System.currentTimeMillis() - now;
        } catch(InterruptedException e){
            checkMessages();
        }
    }

    /** Interrupt above wait*/
    public void wakeWait() {
        synchronized (waitCounter_) {
            waitCounter_.notify();
        }
    }

    /**Accessor function for maxRouterId_*/
    public int getMaxRouterId() {
        return maxRouterId_;
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
            for (int i= 0; i < noControllers_;i++) {
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
							inter.monitoringStart(monitoringAddress, monitoringTimeout);
						}
                    } catch (Exception e) {
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
            } catch(InterruptedException e){
                
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

              ThreadTools.findAllThreads("GC post kill :" + i);

          } catch (java.io.IOException e) {
              System.err.println (leadin() + "Cannot send shut down to local Controller");
              System.err.println (e.getMessage()); 
          } catch (usr.interactor.MCRPException e) {
              System.err.println (leadin() + "Cannot send shut down to local Controller");
              System.err.println (e.getMessage());          
          }
      }
      Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stop messages sent to all controllers");

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping process wrappers");
        Collection <ProcessWrapper> pws= (Collection<ProcessWrapper>)childProcessWrappers_.values();
        for (ProcessWrapper pw: pws) { 
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
        for (int i: getRouterList()) {
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
                startLink(time,gid,dest);   
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
        boolean []visited= new boolean[maxRouterId_+1];
        boolean []visiting= new boolean[maxRouterId_+1];
        for (int i= 0; i < maxRouterId_+1; i++) {
            visited[i]= true;
            visiting[i]= false;
        }
        for (int i= 0; i < nRouters; i++) {
            visited[routerList_.get(i)]= false;
        }
        int []toVisit= new int[nRouters];
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
                startLink(time,l1,l2);
                toVisitCtr++;
                toVisit[0]= l2;
            }
            toVisitCtr--;
            int node= toVisit[toVisitCtr];
            visited[node]= true;
            noVisited++;
            for (int l: getOutLinks(node)) {
                if (visited[l] == false && visiting[l] == false) {
                    toVisit[toVisitCtr]= l;
                    visiting[l]= true;
                    toVisitCtr++;
                }
            }
        }
        
    }
    
    /** Make sure network is connected from r1 to r2*/
    public void connectNetwork(long time,int r1, int r2) 
    {
        int nRouters= routerList_.size();
        if (nRouters <=1)
            return;
        // Boolean arrays are larger than needed but this is fast
        boolean []visited= new boolean[maxRouterId_+1];
        boolean []visiting= new boolean[maxRouterId_+1];
        for (int i= 0; i < maxRouterId_+1; i++) {
            visited[i]= true;
            visiting[i]= false;
        }
        for (int i= 0; i < nRouters; i++) {
            visited[routerList_.get(i)]= false;
        }
        int []toVisit= new int[nRouters];
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
                startLink(time,l1,l2);
                toVisitCtr++;
                toVisit[0]= l2;
            }
            toVisitCtr--;
            int node= toVisit[toVisitCtr];
            visited[node]= true;
            noVisited++;
            for (int l: getOutLinks(node)) {
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

