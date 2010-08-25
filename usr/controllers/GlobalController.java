

package usr.controllers;

import usr.interactor.*;
import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import usr.common.LocalHostInfo;

/**
 * The global controller is in overall control of the software.  It
 * contacts local controllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
public class GlobalController {
    private long simulationTime;
    private String xmlFile_;
    private LocalHostInfo myHostInfo_;
    private ControlOptions options_;
    private boolean listening_;
    private GlobalControllerManagementConsole console_= null;
    private ArrayList <GlobalControllerInteractor> localControllers_ = null;
    private ArrayList <Process> childProcesses_= null;
    private ArrayList <BufferedReader> childOutput_= null;
    private ArrayList <BufferedReader> childError_= null;
    private ArrayList <String> childNames_= null;
    private int aliveCount= 0;
    private EventScheduler scheduler_= null;
    private boolean simulationRunning_= true;

    private Thread ProcessOutputThread_;
    private int noControllers_= 0;
    
    
    
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
      System.out.println("Simulation complete");
      System.out.flush();
      gControl.shutDown();
    }

    public GlobalController () {
    
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
                System.err.println("Out of events to execute");
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
                System.err.println("Out of events to execute!");
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
//                    System.out.println("Check msg true");
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
        return false;
    }
    
    private boolean checkControllerOutput() {
        for (int i=0; i < childOutput_.size();i++) {
            BufferedReader m= childOutput_.get(i);
//            j+=1;
//           System.out.println("Checking for messages from "+j);
            try {
                if (m.ready()) {
                    System.out.println("Remote stdout: "+childNames_.get(i) + 
                      ": "+ m.readLine());
                    return true;
                }    
                
            }
            catch (java.io.IOException e) {
                System.err.println("Error reading output from remote proc");
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
                System.err.println("Error reading output from remote proc");
                System.exit(-1);
            } 
        }
        return false;
    }

    
    private void executeEvent(SimEvent e) {
        System.out.println("Event at "+e.getTime());
    }
    
    private void shutDown() {
        if (!options_.isSimulation()) {
            killAllControllers();
            while (checkMessages()) {};
           // console_.stop();
        }
        System.out.println("All stopped, shut down now!");
        System.exit(-1);
        
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
        scheduler_.addEvent(e);
    }
    
    /** Initialisation steps for when we are using virtual routers
    rather than simulation */
    
    private void initVirtualRouters() {
        noControllers_= options_.noControllers();
        if (options_.startLocalControllers()) {
            
            System.out.println("Starting Local Controllers");
            startLocalControllers();
            try {
                Thread.sleep(5000);  // Simple wait is to
                            // ensure controllers start up
            }
            catch (java.lang.InterruptedException e) {
                System.err.println("initVirtualRouters Got interrupt!");
                System.exit(-1);    
            }
        }
        
        System.out.println("Checking existence of local Controllers");
        checkAllControllers();
    }
    
    private void startLocalControllers() {
        Iterator i= options_.getControllersIterator();
        Process child= null;
        childProcesses_= new ArrayList<Process>();
        childOutput_= new ArrayList<BufferedReader>();
        childError_= new ArrayList<BufferedReader>();
        childNames_= new ArrayList<String>();
        while (i.hasNext()) {
            
            LocalHostInfo lh= (LocalHostInfo)i.next();
            String []cmd= options_.localControllerStartCommand(lh);
            try {
                child = new ProcessBuilder(cmd).start();
            } 
            catch (java.io.IOException e) {
               System.err.println("Unable to execute remote command "+
                 String.valueOf(cmd));
               System.err.println(e.getMessage());
               System.exit(-1);
            }
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
            try {
                Thread.sleep(1000);  // Simple wait is to
                            // ensure controllers start up
            }
            catch (java.lang.InterruptedException e) {
                System.err.println("initVirtualRouters Got interrupt!");
                System.exit(-1);    
            }
        }
    }
    
    public void aliveMessage(LocalHostInfo lh)
    {   
        aliveCount+= 1;
        System.out.println("Got alive from "+lh.getName()+":"+lh.getPort());
        System.out.println("Now alive "+aliveCount);
    }
    
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


     
     public synchronized void wakeUp() {
        notify();  // or All version
     }
     

    /** Check all controllers listed are functioning
    */
    private void checkAllControllers() {
        localControllers_= new ArrayList<GlobalControllerInteractor>();
        GlobalControllerInteractor inter= null;
        for (int i= 0; i < noControllers_;i++) {
           LocalHostInfo lh= options_.getController(i);
           
           try {
              inter= new GlobalControllerInteractor(lh);
           } catch (IOException e) {
              System.err.println("Unable to make connection to "+
                lh.getName()+" "+lh.getPort()+"\n");
              System.err.println(e);
              System.exit(-1);
           }
           
           localControllers_.add(inter);
           try {
             inter.checkLocalController(myHostInfo_);
           } catch (java.io.IOException e) {
           
           } catch (usr.interactor.MCRPException e) {
           
           }
        }
    }
    
    /** Send shutdown message to all controllers
    */
    private void killAllControllers() {
      System.out.println("Stopping all controllers");
      GlobalControllerInteractor inter;
      for (int i= 0; i < noControllers_; i++) {
          inter= localControllers_.get(i);
          try {
              inter.shutDown();
          } catch (java.io.IOException e) {
              System.err.println ("Cannot send shut down to local Controller");
              System.err.println (e.getMessage());
              System.exit(-1);   
          } catch (usr.interactor.MCRPException e) {
              System.err.println ("Cannot send shut down to local Controller");
              System.err.println (e.getMessage());
              System.exit(-1);           
          }
      }
      System.out.println("Stop messages sent to all controllers");
    }

   
   
   
}


