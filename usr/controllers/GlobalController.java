


import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * The global controller is in overall control of the software.  It
 * contacts local controllers to set up virtual routers and then
 * gives set up and tear down instructions directly to them.
 */
class GlobalController {
    private long simulationTime;
    private String xmlFile_;
    private LocalHostInfo myHostInfo_;
    private ControlOptions options_;
    private boolean listening_;
    private GlobalSocketController globalSocketController_ = null;
    private ArrayList <Socket> localControllerSockets_= null;
    private ArrayList <Process> childProcesses_= null;
    private ArrayList <BufferedReader> childOutput_= null;
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
      gControl.shutDown();
    }

    private void init() {
      
      options_= new ControlOptions(xmlFile_);
      
      
      myHostInfo_= new LocalHostInfo(options_.getGlobalPort());  
      if (!options_.isSimulation()) {
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
             if (checkControllerOutput()) {
                 return true;
             }
        }
        return checkLocalControllerMessages();
    }
    
    private boolean checkControllerOutput() {
        Iterator i= childOutput_.iterator();
//        int j=0;
        while (i.hasNext()) {
            BufferedReader m= (BufferedReader)i.next();
//            j+=1;
//           System.out.println("Checking for messages from "+j);
            try {
                if (m.ready()) {
                    System.out.println("REMOTE: "+m.readLine());
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
    
    private boolean checkLocalControllerMessages() {
//        System.out.println("Dequeue payload");
        PayLoad p= globalSocketController_.dequeue();
        if (p == null) {
//            System.out.println("ya got nothing");
            return false;
        }
        if (p.getMessageType() == PayLoad.ALIVE_MESSAGE) {
            LocalHostInfo lhost= (LocalHostInfo) p.getObject();
            aliveMessage(lhost);
        }
        return true;
    }
    
    private void executeEvent(SimEvent e) {
        
    }
    
    private void shutDown() {
        if (!options_.isSimulation()) {
            globalSocketController_.stopListening();
            killAllControllers();
            globalSocketController_.killListenerSockets();
        }
        
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
        startListening();
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
    
    private void startListening() {
        
        java.net.ServerSocket serverSocket= null;
        try {
             serverSocket = new java.net.ServerSocket(myHostInfo_.getPort());
        } catch (java.io.IOException e) {
            System.err.println("Could not listen on port:"+
              String.valueOf(myHostInfo_.getPort()));
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        System.out.println("Starting listener controller");
        globalSocketController_ = new GlobalSocketController(serverSocket,
          this);
        Thread th= new Thread(globalSocketController_);
        th.start();

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
        localControllerSockets_= new ArrayList <Socket> (noControllers_);
        for (int i= 0; i < noControllers_;i++) {
           LocalHostInfo lh= options_.getController(i);
           Socket skt= null;
           try {
               skt= new Socket(lh.getIp(),lh.getPort());
           } catch (IOException e) {
               System.err.println("Cannot open socket to "+lh.getName() +
                 ":"+String.valueOf(lh.getPort()));
               System.exit(-1);
           
           }
           checkController(skt);
           localControllerSockets_.add(skt);
        }
    }
    
    /** Send shutdown message to all controllers
    */
    private void killAllControllers() {
      System.out.println("Stopping all controllers");
      for (int i= 0; i < noControllers_; i++) {
          PayLoad pl= new PayLoad(PayLoad.SHUTDOWN_MESSAGE,null);
          pl.sendPayLoad(localControllerSockets_.get(i));
          
      }
    }

    /** Check local controller is up and functioning 
    */
    private void checkController(Socket skt) {
        PayLoad dg= new PayLoad(PayLoad.ALIVE_MESSAGE,myHostInfo_);
        dg.sendPayLoad(skt);
        
    }
    
   
   
}


