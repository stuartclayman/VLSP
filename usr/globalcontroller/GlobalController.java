package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
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

/**
 * The GlobalController is in overall control of the software.  It
 * contacts LocalControllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
public class GlobalController implements ComponentController {
    private long simulationTime;
    private long simulationStartTime;
    private long lastEventLength;
    private String xmlFile_;
    private LocalHostInfo myHostInfo_;
    private ControlOptions options_;
    private boolean listening_;
    private GlobalControllerManagementConsole console_= null;
    private ArrayList <LocalControllerInteractor> localControllers_ = null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;
    private HashMap<Pair<Integer,Integer>,Integer> linkWeights_= null;
    private HashMap <Integer, ArrayList<Integer>> outLinks_= null;
    private HashMap <Integer, ArrayList<Integer>> inLinks_= null;
    private ArrayList <Integer> routerList_= null;
    private ArrayList <String> childNames_= null;
    private HashMap <LocalControllerInfo, LocalControllerInteractor> interactorMap_= null;
    private HashMap <Integer, BasicRouterInfo> routerIdMap_= null;
    private HashMap <LocalControllerInfo, PortPool> portPools_= null;
    private int aliveCount= 0;
    private EventScheduler scheduler_= null;
    private boolean simulationRunning_= true;
    private int maxRouterId_=0;
    private RouterOptions routerOptions_= null;

    private Thread ProcessOutputThread_;
    private int noControllers_= 0;
    
    private String myName = "GlobalController";

    private APController APController_= null;
    
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
      gControl.simulate();
      System.out.println(gControl.leadin() + "Simulation complete");
      System.out.flush();
    }

    /**
     * Construct a GlobalController -- this constructor contains things
     which apply whether we are simulation or 
     */
    public GlobalController () {
    }

    private void init() {
      linkWeights_ = new HashMap<Pair<Integer,Integer>,Integer> ();
      outLinks_= new HashMap <Integer, ArrayList<Integer>> ();
      inLinks_= new HashMap<Integer, ArrayList<Integer>> ();
      routerList_= new ArrayList<Integer>();
      options_= new ControlOptions(xmlFile_);
      routerOptions_= options_.getRouterOptions();
      APController_= ConstructAPController.constructAPController
        (routerOptions_);
      myHostInfo_= new LocalHostInfo(options_.getGlobalPort());  
      if (!options_.isSimulation()) {
          initEmulation();
      }
      initSchedule();
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
            
              System.out.println(leadin() + "Starting Local Controllers");
              startLocalControllers();
          }        
          System.out.println(leadin() + "Checking existence of local Controllers");
          checkAllControllers();
    }
    
    /** Main loop for events */
    private void simulate() {
        if (options_.isSimulation()) {
            simulateSoftware();
        } else {
            simulateHardware();
        }
    
    }
    
    /** Main loop for events if software simulation */
    private void simulateSoftware() {
        long time= 0;
        while (simulationRunning_) {
            SimEvent e= scheduler_.getFirstEvent();
            if (e == null) {
                System.err.println(leadin() + "Out of events to execute");
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
                System.err.println(leadin() + "Out of events to execute!");
                break;
            }
            while(simulationRunning_) {
                long eventTime= e.getTime();
                simulationTime= System.currentTimeMillis();
                
                if (simulationTime - (simulationStartTime + eventTime) > 
                     options_.getMaxLag()) {
                    System.err.println(leadin() +
                      "Simulation lagging too much, slow down events");
                    bailOut();
                }
                if ((simulationStartTime + eventTime) <= simulationTime) {
                    executeEvent(e);
                    break;
                }
                
                
                if (checkMessages()) {
                    // System.out.println(leadin() + "Check msg true");
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
        System.err.println(leadin() + "TODO -- need to deal with event here!");
        return true;
    }
    
    

     /** bail out of simulation relatively gracefully */
    private void bailOut() {
        System.err.println(leadin() + "Bailing out of simulation!");
        shutDown();
        System.err.println(leadin() + "Exit after bailout");
        System.out.println(leadin() + "Bailing out of simulation!");
        System.exit(-1);
    }
    
    private void executeEvent(SimEvent e) {
        Object extraParms= null;
        long eventBegin = System.currentTimeMillis();
        options_.preceedEvent(e,scheduler_,this);
        System.out.println("SIMULATION: " + "<" + lastEventLength + "> " +
                           e.getTime() + " @ " + 
                           eventBegin +  " => " +  e);
 
        int type= e.getType();
        try {
            if (type == SimEvent.EVENT_START_SIMULATION) {
                startSimulation();
            }
            else if (type == SimEvent.EVENT_END_SIMULATION) {
                endSimulation();
            }
            else if (type == SimEvent.EVENT_START_ROUTER) {
                
                startRouter();
            }
            else if (type == SimEvent.EVENT_END_ROUTER) {
                int routerNo= (Integer)e.getData();
                endRouter(routerNo);
            }
            else if (type == SimEvent.EVENT_START_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                startLink(router1, router2);
            }
            else if (type == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                endLink(router1, router2);
            }
            else {
                System.err.println(leadin() + "Unexected event type "
                  +type+" shutting down!");
                shutDown();
                return;
            }
            long eventEnd = System.currentTimeMillis();
            lastEventLength = eventEnd - eventBegin;

        } catch (ClassCastException ex) {
            System.err.println(leadin() + "Event "+type+" had wrong object");
            shutDown();
            return;
        }
        options_.followEvent(e,scheduler_,this, extraParms);

    }
    
    /** Event for start Simulation */
    private void startSimulation() {
        
        lastEventLength = 0;
        System.out.println(leadin() + "Start of simulation event at: " +  
            System.currentTimeMillis());    
    }
    
    /** Event for end Simulation */
    private void endSimulation() {
        System.out.println(leadin() + "End of simulation event at " + 
          System.currentTimeMillis());
        simulationRunning_= false;
        shutDown();
    }
    
    /** Register existence of router */
    private void registerRouter(int rId) 
    {
        routerList_.add(rId);
    }
    
    /** Event to start a router */
    private void startRouter() {
        maxRouterId_++;
        int rId= maxRouterId_;
        
        registerRouter(rId);
        
        if (options_.isSimulation()) {
            System.err.println(leadin() + "TODO write simulated router code");
        } else {
            startVirtualRouter(maxRouterId_);
        }
    }
    
    private void startVirtualRouter(int id) 
    {
        // Find least used local controller

        LocalControllerInfo lc;
        LocalControllerInfo leastUsed= options_.getController(0);
        double minUse= leastUsed.getUsage();
        double thisUsage;
        for (int i= 1; i < noControllers_; i++) {
            lc= options_.getController(i);
            thisUsage= lc.getUsage();
            // System.out.println(i+" Usage "+thisUsage);
            if (thisUsage == 0.0) {
               leastUsed= lc;
               break;
            }
            if (thisUsage < minUse) {
                minUse= thisUsage;
                leastUsed= lc;
            }
        }
        LocalControllerInteractor lci= interactorMap_.get(leastUsed);
        PortPool pp= portPools_.get(leastUsed);
        try {
            int port= pp.findPort(2);
            leastUsed.addRouter();  // Increment count
            
            System.out.println(leadin() + "Creating router " + id + " on " + leastUsed);

            // create the new router and get it's name
            String routerName = lci.newRouter(id, port);

            BasicRouterInfo br= new BasicRouterInfo(id,simulationTime, leastUsed,port);
            br.setName(routerName);

            // keep a handle on this router
            routerIdMap_.put(id,br);

            System.out.println(leadin() + "Created router " + routerName);

        } catch (IOException e) {
            System.err.println(leadin() +"Could not start new router on " + leastUsed);
            System.err.println(e.getMessage());
            bailOut();
        } catch (MCRPException e) {
            System.err.println(leadin() + "Could not start new router on " + leastUsed);
            System.err.println(e.getMessage());
            bailOut();
        }
        
    }
    
    /** Unregister a router and all links from structures in 
        GlobalController*/
    private void unregisterRouter(int rId)
    {
        ArrayList<Integer> outBound= outLinks_.get(rId);
        if (outBound != null) {
            Object [] out2= outBound.toArray();
            for (Object i: out2) {
                unregisterLink(rId,(Integer)i);
                //System.err.println("Unregister link "+rId+" "+i);
            }
        }
        int index= routerList_.indexOf(rId);
        //System.err.println("Router found at index "+index);
        routerList_.remove(index);
    }
    
    /** remove a router in simulation*/
    private void endSimulationRouter(int rId) {
         
    }
    
    /** Send shutdown to a virtual router */
    private void endVirtualRouter(int rId) {
        BasicRouterInfo br= routerIdMap_.get(rId);
        LocalControllerInteractor lci= interactorMap_.get
            (br.getLocalControllerInfo());
        try {
            lci.endRouter(br.getHost(),br.getManagementPort());
        } catch (Exception e) {
            System.err.println(leadin()+ "Cannot shut down router "+
              br.getHost()+":"+br.getManagementPort());
            bailOut();
        }
        routerIdMap_.remove(rId);
    }
    
    /** Event to end a router */
    private void endRouter(int routerId) {
        
        if (options_.isSimulation()) {
            endSimulationRouter(routerId);
        } else {
            endVirtualRouter(routerId);
        }
        unregisterRouter(routerId);
    }
    
    /** Register a link with structures necessary in Global
    Controller */
    private void registerLink(int router1Id, int router2Id) 
    {
        Pair <Integer, Integer> rnos= makeRouterPair(router1Id,router2Id);
        linkWeights_.put(rnos,1);
        ArrayList <Integer> out;
        ArrayList <Integer> in;
        ArrayList <Integer> newArray;
        //System.err.println(leadin()+" Adding link from "+router1Id+" "+router2Id);
        // Set outlinks from router1 Id
        out= outLinks_.get(router1Id);
        if (out == null) {
            newArray = new ArrayList<Integer>();
            newArray.add(router2Id);
            outLinks_.put(router1Id,newArray);
        } else {
            out.add((Integer)router2Id);
            //System.err.println("Links now ");
            //for (Integer i: out) {
            //    System.err.println(router1Id+" "+i);
            //}
            //outLinks_.put(router1Id,out);
        }
        // Set outlinks from router2 Id
        out= outLinks_.get(router2Id);
        if (out == null) {
            newArray = new ArrayList<Integer>();
            newArray.add(router1Id);
            outLinks_.put(router2Id,newArray);
        } else {
            out.add((Integer)router1Id);
            //outLinks_.put(router2Id,out);
        }
        // Set inlinks to router1Id
        in= inLinks_.get(router1Id);
        if (in == null) {
            newArray= new ArrayList<Integer>();
            newArray.add(router2Id);
            inLinks_.put(router1Id,newArray);
        } else {
            in.add((Integer)router2Id);
            //inLinks_.put(router1Id,in);
        }
        // Set inlinks to router2Id
        in= inLinks_.get(router2Id);
        if (in == null) {
            newArray= new ArrayList<Integer>();
            newArray.add(router1Id);
            inLinks_.put(router2Id,newArray);
        } else {
            in.add((Integer)router1Id);
            //inLinks_.put(router2Id,in);
        }    
    }
    
    /** Event to link two routers */
    private void startLink(int router1Id, int router2Id) {
        //System.err.println("Start link "+router1Id+" "+router2Id);

        // check if this link already exists
        ArrayList<Integer> outForRouter1 = outLinks_.get(router1Id);

        if (outForRouter1 != null && outForRouter1.contains(router2Id)) {
            // we already have this link
            System.err.println(leadin() + "Link already exists: "+router1Id + " -> " + router2Id);
        } else {
            if (options_.isSimulation()) {
                startSimulationLink(router1Id, router2Id);       
            } else {
                startVirtualLink(router1Id, router2Id);
            }
            registerLink(router1Id, router2Id);
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
        //System.out.println("Got router Ids"+br1.getHost()+br2.getHost());
        
        lc= br1.getLocalControllerInfo();
        //System.out.println("Got LC");
        lci= interactorMap_.get(lc);
        //System.out.println("Got LCI");
        System.out.println(leadin() + "Global controller linking routers "+
            br1 + " and "+ br2);
        try {
            String connectionName = lci.connectRouters(br1.getHost(), br1.getManagementPort(),
               br2.getHost(), br2.getManagementPort());
            System.out.println(leadin() + br1 + " -> " + br2 + " = " + connectionName);
        } catch (IOException e) {
            System.err.println(leadin() + "Cannot link routers");
            System.err.println(leadin() + e.getMessage());
            bailOut();
        }
        catch (MCRPException e) {
            System.err.println(leadin() + "Cannot link routers");
            System.err.println(leadin() + e.getMessage());
            bailOut();
        }

    }
    
    /* Return a list of outlinks from a router */
    public List<Integer> getOutLinks(int routerId)
    {
        return outLinks_.get(routerId);
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
    
    /** Register a link with structures necessary in Global
    Controller */
    private void unregisterLink(int router1Id, int router2Id) 
    {
        Pair <Integer, Integer> rnos= makeRouterPair(router1Id,router2Id);
        linkWeights_.remove(rnos);
        ArrayList <Integer> out;
        ArrayList <Integer> in;
        int arrayPos;
        //System.out.println(leadin()+"Adding link from "+router1Id+" "+router2Id);
        // remove r2 from outlinks of r1
        out= outLinks_.get(router1Id);
        arrayPos= out.indexOf(router2Id);
        out.remove(arrayPos);
        //outLinks_.put(router1Id,out);
        // remove r1 from outlinks of r2
        out= outLinks_.get(router2Id);
        arrayPos= out.indexOf(router1Id);
        out.remove(arrayPos);
        //outLinks_.put(router2Id,out);
        // remove r2 from inlinks to r1
        in= inLinks_.get(router1Id);
        arrayPos= in.indexOf(router2Id);
        in.remove(arrayPos);
        //inLinks_.put(router1Id,in);
        // Set inlinks to router1Id
        in= inLinks_.get(router2Id);
        arrayPos= in.indexOf(router1Id);
        in.remove(arrayPos);
        //inLinks_.put(router2Id,in); 
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
        try {
            lci.endLink(br1.getHost(),br1.getManagementPort(),rId2);
        } catch (Exception e) {
            System.err.println(leadin()+ "Cannot shut down link "+
              br1.getHost()+":"+br1.getManagementPort()+" " +
              br2.getHost()+":"+br2.getManagementPort());
            bailOut();
        }
    }
    
    /** Event to unlink two routers */
    private void endLink (int router1Id, int router2Id) {
        unregisterLink(router1Id, router2Id);
        if (options_.isSimulation()) {
            endSimulationLink(router1Id, router2Id);
        } else {
            endVirtualLink(router1Id, router2Id);
        }
    }
    
    /** Shutdown called from console -- add shut down command to list to
    happen now */
    public synchronized void shutDownCommand() {
        System.out.println(leadin()+"Shut down called from console");
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION,0,null);
        scheduler_.addEvent(e);
        notifyAll();
    }  
      
    private void shutDown() {
        System.out.println (leadin() + "SHUTDOWN CALLED!");
        if (!options_.isSimulation()) {

            ThreadTools.findAllThreads("GC pre killAllControllers:");

            killAllControllers();

            ThreadTools.findAllThreads("GC post killAllControllers:");

            while (checkMessages()) {};

            System.out.println(leadin()+ "Pausing.");

            try {
               Thread.sleep(10);
            } catch (Exception e) {
                System.err.println(leadin()+ e.getMessage());
                System.exit(-1);
            }

            ThreadTools.findAllThreads("GC post checkMessages:");

            System.out.println(leadin()+"Stopping console");
            console_.stop();
            ThreadTools.findAllThreads("GC post stop console:");
        }
        System.out.println(leadin() + "All stopped, shut down now!");

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
            String []cmd= options_.localControllerStartCommand(lh);
            try {
                System.out.println(leadin() + "Starting process " + Arrays.asList(cmd));
                child = new ProcessBuilder(cmd).start();
            } catch (java.io.IOException e) {
                System.err.println(leadin() + "Unable to execute remote command "+ Arrays.asList(cmd));
               System.err.println(e.getMessage());
               System.exit(-1);
            }

            String procName = lh.getName()+":"+lh.getPort();
            childNames_.add(procName);
            childProcessWrappers_.put(procName, new ProcessWrapper(child, procName));

            try {
                Thread.sleep(10);  // Simple wait is to
                            // ensure controllers start up
            }
            catch (java.lang.InterruptedException e) {
                System.err.println(leadin() + "initVirtualRouters Got interrupt!");
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
        System.out.println(leadin() + "Received alive count from "+
          lh.getName()+":"+lh.getPort());
    }
    
    /**
     * Wait until a specified absolute time is milliseconds.
     */
    public synchronized void waitUntil(long time){
        long now = System.currentTimeMillis();

        if (time <= now)
            return;
        try {
            long timeout = time - now;

            System.out.println("SIMULATION: " +  "<" + lastEventLength + "> " +
                               (now - simulationStartTime) + " @ " + 
                               now +  " waiting " + timeout);
            wait(timeout);

            lastEventLength = System.currentTimeMillis() - now;
        } catch(InterruptedException e){
            checkMessages();
        }
    }

    /**Accessor function for maxRouterId_*/
    public int getMaxRouterId() {
        return maxRouterId_;
    }

    /**
     * Wakeup the controller.
     */
    public synchronized void wakeUp() {
        notify();  // or All version
    }
     

    /** 
     * Check all controllers listed are functioning and
     * creates interactors with the LocalControllers.
    */
    private synchronized void checkAllControllers() {
        // try 10 times, with 500 millisecond gap
        int MAX_TRIES = 10;
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
                        System.out.println(leadin() + "Trying to make connection to "+
                           lh.getName()+" "+lh.getPort());
                        inter= new LocalControllerInteractor(lh);
                        
                        localControllers_.add(inter);
                        interactorMap_.put(lh,inter);
                        inter.checkLocalController(myHostInfo_);
                        if (options_.getRouterOptionsString() != "") {
                            inter.setConfigString(options_.getRouterOptionsString());
                        }
                    } catch (Exception e) {
                    }
           
                }
            }

            // check if we have connected to all of them
            // check if the no of controllers == the no of interactors
            // if so, we dont have to do all lopps
            if (noControllers_ == localControllers_.size()) {
                System.out.println(leadin() + "All LocalControllers connected after " + (tries+1) + " tries");
                isOK = true;
                break;
            }
        }

        // if we did all loops and it's not OK
        if (!isOK) {
            // couldnt reach all LocalControllers
            // We can keep a list of failures if we need to.
            System.err.println(leadin() + "Can't talk to all LocalControllers");
            bailOut();
        }


        // Wait to see if we have all controllers.
        for (int i= 0; i < options_.getControllerWaitTime(); i++) {
            while (checkMessages()) {};
            if (aliveCount == noControllers_) {
                System.out.println(leadin() + "All controllers responded with alive message.");
                return;
            }
            try {
                Thread.sleep(500);
            } catch(InterruptedException e){
                
            }
            
        }
        System.err.println(leadin() + "Only "+aliveCount+" from "+noControllers_+
                           " local Controllers responded.");
        bailOut();
    }
    
    /** 
     * Send shutdown message to all controllers
     */
    private void killAllControllers() {
      if (localControllers_ == null) 
          return;
      System.out.println(leadin() + "Stopping all controllers");
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
      System.out.println(leadin() + "Stop messages sent to all controllers");

        System.out.println(leadin() + "Stopping process wrappers");
        Collection <ProcessWrapper> pws= (Collection<ProcessWrapper>)childProcessWrappers_.values();
        for (ProcessWrapper pw: pws) { 
            pw.stop();
        }

    }
    
    public int getLinkWeight(int l1, int l2) 
    /** Return the weight from link1 to link2 */
    {
        Integer w;
        Pair<Integer,Integer> links= null;
        if (l1 < l2) {
            links= new Pair<Integer,Integer>(l1,l2);
        } else {
            links= new Pair<Integer,Integer>(l2,l1);
        }
        w= linkWeights_.get(links);
        if (w == null)
            return 0;
        return w;
    }

    /** Return a list of node ids */
    public List<Integer> getNodeList() {
        Set<Integer> n=  routerIdMap_.keySet();
        return new ArrayList<Integer>(n);
       
    }

 

    /** Accessor function for ControlOptions structure options_ */
    public ControlOptions getOptions() {
        return options_;
    }

    /**
     * Get the name of this GlobalController.
     */
    public String getName() {
        return myName + ":" + myHostInfo_.getPort();
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String GC = "GC: ";

        return getName() + " " + GC;
    }



   
}


