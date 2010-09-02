

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

/**
 * The GlobalController is in overall control of the software.  It
 * contacts LocalControllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
public class GlobalController implements ComponentController {
    private long simulationTime;
    private String xmlFile_;
    private LocalHostInfo myHostInfo_;
    private ControlOptions options_;
    private boolean listening_;
    private GlobalControllerManagementConsole console_= null;
    private ArrayList <LocalControllerInteractor> localControllers_ = null;
    ///private ArrayList <Process> childProcesses_= null;
    ///private ArrayList <BufferedReader> childOutput_= null;
    ///private ArrayList <BufferedReader> childError_= null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;
    private ArrayList <String> childNames_= null;
    private HashMap <LocalControllerInfo, LocalControllerInteractor> interactorMap_= null;
    private int aliveCount= 0;
    private EventScheduler scheduler_= null;
    private boolean simulationRunning_= true;
    private int maxRouterId_=0;

    private Thread ProcessOutputThread_;
    private int noControllers_= 0;
    
    private String myName = "GlobalController";

    
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
     * Construct a GlobalController.
     */
    public GlobalController () {
        ///childProcesses_= new ArrayList<Process>();
        ///childOutput_= new ArrayList<BufferedReader>();
        ///childError_= new ArrayList<BufferedReader>();
        childProcessWrappers_ = new HashMap<String, ProcessWrapper>();
        childNames_= new ArrayList<String>();
    }

    private void init() {
      
      options_= new ControlOptions(xmlFile_);
      
      
      myHostInfo_= new LocalHostInfo(options_.getGlobalPort());  
      if (!options_.isSimulation()) {
          console_= new GlobalControllerManagementConsole(this,myHostInfo_.getPort());
          console_.start();
          initVirtualRouters();
      }
      initSchedule();
    }
    
    private void simulate() {
        if (options_.isSimulation()) {
            simulateSoftware();
        } else {
            simulateHardware();
        }
    
    }
    
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
    
    private void simulateHardware() {
    
        while (simulationRunning_) {
            SimEvent e= scheduler_.getFirstEvent();
            if (e == null) {
                System.err.println(leadin() + "Out of events to execute!");
                break;
            }
            while(simulationRunning_) {
                long eventTime= e.getTime();
                simulationTime= System.currentTimeMillis();
                if (eventTime <= simulationTime) {
                    executeEvent(e);
                    break;
                }
                if (checkMessages()) {
//                    System.out.println(leadin() + "Check msg true");
                    continue;
                }
                waitUntil(eventTime);
            }    
        }  
    }
    
    private boolean checkMessages() {
        if (options_.startLocalControllers()) {
             return checkControllerOutput();
        }
        return checkQueueMessages();
        
    }
    
    private boolean checkQueueMessages()
    {
        BlockingQueue<Request> queue = console_.queue();
        if (queue.size() == 0)
           return false;
        Request req= queue.remove();
        System.err.println(leadin() + "TODO -- need to deal with event here!");
        return true;
    }
    
    private boolean checkControllerOutput() {
        return false;
    }

    /*
    private boolean checkControllerOutput() {
        for (int i=0; i < childOutput_.size();i++) {
            BufferedReader m= childOutput_.get(i);
//            j+=1;
//           System.out.println(leadin() + "Checking for messages from "+j);
            try {
                if (m.ready()) {
                    System.out.println("Remote stdout: "+childNames_.get(i) + 
                      ": "+ m.readLine());
                    return true;
                }    
                
            }
            catch (java.io.IOException e) {
                System.err.println(leadin() + "Error reading output from remote proc");
                System.exit(-1);
            } 
            m= childError_.get(i);
//            j+=1;
//           System.out.println("Checking for messages from "+j);
            try {
                if (m.ready()) {
                    System.out.println("Remote stderr: "+childNames_.get(i) + 
                      ": "+ m.readLine());
                    return true;
                }    
                
            }
            catch (java.io.IOException e) {
                System.err.println(leadin() + "Error reading output from remote proc");
                System.exit(-1);
            } 
        }
        return false;
    }
    */

    
    private void executeEvent(SimEvent e) {
        int type= e.getType();
        try {
            if (type == SimEvent.EVENT_START_SIMULATION) {
                startSimulation();
                return;
            }
            if (type == SimEvent.EVENT_END_SIMULATION) {
                endSimulation();
                return;
            }
            if (type == SimEvent.EVENT_START_ROUTER) {
                
                startRouter();
                return;
            }
            if (type == SimEvent.EVENT_END_ROUTER) {
                int routerNo= (Integer)e.getData();
                endRouter(routerNo);
                return;
            }
            if (type == SimEvent.EVENT_START_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                startLink(router1, router2);
                
                return;
            }
            if (type == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                endLink(router1, router2);
                return;
            }
            System.err.println(leadin() + "Unexected event type "+type+" shutting down!");
            shutDown();
        } catch (ClassCastException ex) {
            System.err.println(leadin() + "Event "+type+" had wrong object");
            shutDown();
        }
    }
    
    /** Event for start Simulation */
    private void startSimulation() {
    
    }
    
    /** Event for end Simulation */
    private void endSimulation() {
        simulationRunning_= false;
        shutDown();
    }
    
    /** Event to start a router */
    private void startRouter() {
        if (options_.isSimulation()) {
            System.err.println(leadin() + "TODO write simulated router code");
        } else {
            startVirtualRouter();
        }
    }
    
    private void bailOut() {
        System.err.println(leadin() + "Bailing out of simulation!");
        shutDown();
        System.exit(-1);
    }
    
    private void startVirtualRouter() 
    {
        // Find least used local controller

        LocalControllerInfo lc;
        LocalControllerInfo leastUsed= options_.getController(0);
        double minUse= leastUsed.getUsage();
        double thisUsage;
        for (int i= 1; i < noControllers_; i++) {
            lc= options_.getController(i);
            thisUsage= lc.getUsage();
//            System.out.println(i+" Usage "+thisUsage);
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
        leastUsed.addRouter();  // Increment count
        maxRouterId_++;
        try {
            lci.newRouter(maxRouterId_);
        } catch (IOException e) {
            System.err.println(leadin() +"Could not start new router");
            System.err.println(e.getMessage());
            bailOut();
        } catch (MCRPException e) {
            System.err.println(leadin() + "Could not start new router");
            System.err.println(e.getMessage());
            bailOut();
        }
        
    }
    
    /** Event to end a router */
    private void endRouter(int routerId) {
    
    }
    
    /** Event to link two routers */
    private void startLink(int router1Id, int router2Id) {
    
    }
    
    /** Event to unlink two routers */
    private void endLink (int router1Id, int router2Id) {
    
    }
    
    
    private void shutDown() {
        System.out.println (leadin() + "SHUTDOWN CALLED!");
        if (!options_.isSimulation()) {
            killAllControllers();
            while (checkMessages()) {};
            System.out.println(leadin()+ "Local controller shutdown called.  Pausing.");
            try {
               Thread.sleep(5000);
            } catch (Exception e) {
                System.err.println(leadin()+ e.getMessage());
                System.exit(-1);
            }
            System.out.println(leadin()+"Stopping console");
            console_.stop();
        }
        System.out.println(leadin() + "All stopped, shut down now!");

    }
    
    private void initSchedule() {
        long time;
        if (options_.isSimulation()) {
            time= options_.getSimulationLength();
        } else {
            time= EventScheduler.afterPause(options_.getSimulationLength());
        }
        scheduler_= new EventScheduler();
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION,time,null);
        
        // TODO remove this hack.
        SimEvent e2= new SimEvent(SimEvent.EVENT_START_ROUTER,
            EventScheduler.afterPause(options_.getSimulationLength())/2,null);
        SimEvent e3= new SimEvent(SimEvent.EVENT_START_ROUTER,
            EventScheduler.afterPause(2*options_.getSimulationLength())/3,null);
        scheduler_.addEvent(e);
        scheduler_.addEvent(e2);
        //scheduler_.addEvent(e3);
    }
    
    /** Initialisation steps for when we are using virtual routers
    rather than simulation */
    
    private void initVirtualRouters() {
        noControllers_= options_.noControllers();
        if (options_.startLocalControllers()) {
            
            System.out.println(leadin() + "Starting Local Controllers");
            startLocalControllers();
            try {
                Thread.sleep(5000);  // Simple wait is to
                            // ensure controllers start up
            }
            catch (java.lang.InterruptedException e) {
                System.err.println(leadin() + "initVirtualRouters Got interrupt!");
                System.exit(-1);    
            }
        }
        
        System.out.println(leadin() + "Checking existence of local Controllers");
        checkAllControllers();
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

            /* old approach
            childProcesses_.add(child);
            InputStream is= child.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br= new BufferedReader(isr);
            childOutput_.add(br);
            is= child.getErrorStream();
            isr = new InputStreamReader(is);
            br= new BufferedReader(isr);
            childError_.add(br);
            childNames_.add(lh.getName()+":"+lh.getPort());
            */

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
        System.out.println(leadin() + "Received alive count from "+lh.getName()+":"+lh.getPort());
    }
    
    /**
     * Wait until a specified absolute time is milliseconds.
     */
    public synchronized void waitUntil(long time){
        long now= System. currentTimeMillis();
        if (time <= now)
            return;
        try {
            wait(time - now);
        } catch(InterruptedException e){
            checkMessages();
        }
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
        localControllers_= new ArrayList<LocalControllerInteractor>();
        interactorMap_= new HashMap<LocalControllerInfo, LocalControllerInteractor>();
        LocalControllerInteractor inter= null;
        for (int i= 0; i < noControllers_;i++) {
           LocalControllerInfo lh= options_.getController(i);
           
           try {
              inter= new LocalControllerInteractor(lh);
           } catch (IOException e) {
               System.err.println(leadin() + "Unable to make connection to "+
                lh.getName()+" "+lh.getPort()+"\n");
              System.err.println(leadin() + e.getMessage());
              bailOut();
           }
           
           localControllers_.add(inter);
           interactorMap_.put(lh,inter);
           try {
             inter.checkLocalController(myHostInfo_);
           } catch (java.io.IOException e) {
           
           } catch (usr.interactor.MCRPException e) {
           
           }
        }
        
        // Wait to see if we have all controllers.
        for (int i= 0; i < options_.getControllerWaitTime(); i++) {
            while (checkMessages()) {};
            if (aliveCount == noControllers_) {
                System.out.println(leadin() + "All controllers responded with alive message.");
               return;
            }
            try {
                Thread.sleep(1000);
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
          } catch (java.io.IOException e) {
              System.err.println (leadin() + "Cannot send shut down to local Controller");
              System.err.println (e.getMessage()); 
          } catch (usr.interactor.MCRPException e) {
              System.err.println (leadin() + "Cannot send shut down to local Controller");
              System.err.println (e.getMessage());          
          }
      }
      System.out.println(leadin() + "Stop messages sent to all controllers");
    }

    protected void finalize() {
        killAllControllers();
   
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


