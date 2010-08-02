/** This class contains the options used for a simulation.  It reads in an
 * XML file or string to generate them
 * The options specify hosts and controls used in simulation
 */
package usr.controllers;

import java.util.ArrayList;
import java.util.Iterator;

class ControlOptions {
    private ArrayList<LocalHostInfo> localControllers_;
    private int globalControlPort_;
    private int simulationLength_= 10000;
    private String remoteLoginCommand_= null;
    private String remoteStartController_= null;
    private String remoteLoginFlags_ = null;
    private String remoteLoginUser_= null;
    private boolean startLocalControllers_= true;
    private boolean isSimulation_= false;


    /** init function sets up basic information */
    public void init () {
      
      localControllers_= new ArrayList<LocalHostInfo>();
      
      // Temporary bodge for testing TODO Fix.
      globalControlPort_= 8888;
      remoteLoginCommand_ = "/usr/bin/ssh";
      remoteLoginFlags_ = "-n";
      remoteLoginUser_="richard";
      remoteStartController_ = 
        "java -cp /home/richard/code/userspacerouter usr.controllers.LocalController";
      LocalHostInfo tmp= new LocalHostInfo(4000);
      addNewHost(tmp);
      for (int i= 4001; i < 4003;i++) {
          tmp= new LocalHostInfo(i);
          addNewHost(tmp);
      }

    }
    
    /** Adds information about a new host to the list
    */
    private void addNewHost(LocalHostInfo host) {
      localControllers_.add(host);
    }

    /** Read control options from XML file 
    */
    public ControlOptions (String fName) {
      System.out.println("To write function to read "+fName);
      init();
     
    }
    
    /** Return string to launch local controller on remote
    machine given machine name 
    */
    public String [] localControllerStartCommand(LocalHostInfo lh) {
        String [] cmd= new String[5];
        cmd[0] = remoteLoginCommand_;
        cmd[1] = remoteLoginFlags_;
        if (remoteLoginUser_ == null) {
            cmd[2]=lh.getName();
        } else {
            cmd[2] = remoteLoginUser_+"@"+lh.getName();
        }
        cmd[3] = remoteStartController_;
        cmd[4] = String.valueOf(lh.getPort());
        return cmd;
    }
    
    /** Accessor function returns the number of controllers 
    */
    public int noControllers() {
      return localControllers_.size();
    }
    
    /** Accessor function returns the i th controller 
    */
    public LocalHostInfo getController(int i) {
      return localControllers_.get(i);
    }
    
    public Iterator getControllersIterator() {
        return localControllers_.iterator();
    }
 
    /** Should global controller attempt to remotely start local
      controllers using ssh or assume it has been done.
    */
    public boolean startLocalControllers() {
        return startLocalControllers_;
    }
    
    /** Are we simulating nodes or executing them with virtual
    routers 
    */
    public boolean isSimulation() {
        return isSimulation_;
    }
    
    /** Return port number for global controller 
    */
    public int getGlobalPort() {
      return globalControlPort_;
    }   
   
    /** Return length of simulation
    */
    
    public int getSimulationLength() {
        return simulationLength_;
    }
}

