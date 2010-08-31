package usr.localcontroller;

import java.lang.*;
import java.util.*;
import java.io.IOException;
import java.net.*;
import usr.console.*;
import usr.router.*;
import usr.common.LocalHostInfo;
import usr.common.ProcessWrapper;
import usr.interactor.*;


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
    private LocalHostInfo globalController_;
    private Thread listenThread_;
    private GlobalControllerInteractor gcInteractor_= null;
    private LocalControllerManagementConsole console_= null;
    private boolean listening_ = true;
    private int maxPort_= 20000;  // Ports in use
    private ArrayList <BasicRouterInfo> routers_= null;
    private ArrayList <RouterInteractor> routerInteractors_ = null;
    private ArrayList <Router> routerList_= null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;

    private String myName = "LocalController";
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        
        LocalController self_;
        
        if (args.length != 1) {
            System.err.println("Command line must specify "+
              "port number to listen on and nothing else.");
            System.exit(-1);
        }
        int port= 0;
        
        try {
          port= Integer.parseInt(args[0]);
          if (port < 0) 
            throw new NumberFormatException ("Port number must be > 0");
        }
        catch (NumberFormatException e) {
          System.err.println("Unable to understand port number."+
            e.getMessage());
          System.exit(-1);
        }
        self_= new LocalController(port);
        

    }
    
    /** Constructor for local controller starting on port */
    public LocalController (int port) {
        hostInfo_= new LocalControllerInfo(port);
        routers_= new ArrayList<BasicRouterInfo>();
        routerList_= new ArrayList<Router>();
        childProcessWrappers_ = new HashMap<String, ProcessWrapper>();
        routerInteractors_ = new ArrayList<RouterInteractor>();
        console_= new LocalControllerManagementConsole(this, port);
        console_.start();
    }
    
    /** Received shut Down data gram from global */
    public void shutDown() {

        System.out.println("Local controller got shutdown message from global controller.");
        System.out.println("Stopping all running routers"); 
        // TODO send proper shut down
        for (int i= 0; i < routers_.size(); i++) {
            ///Router r= routerList_.get(i);
            ///r.stop();

            RouterInteractor interactor = routerInteractors_.get(i);
            try {
                interactor.shutDown();
            } catch (java.io.IOException e) {
                System.err.println (leadin() + "Cannot send shut down to Router");
                System.err.println (e.getMessage()); 
            } catch (usr.interactor.MCRPException e) {
                System.err.println (leadin() + "Cannot send shut down to Router");
                System.err.println (e.getMessage());          
            }

        }
        console_.stop();
        System.exit(-1);
    }
    
    /**
     * Get the host info the the host this is a LocalController for.
     */
    public LocalControllerInfo getHostInfo() {
        return hostInfo_;
    }
    
    /** 
     * Received alive message from GlobalController. 
     */
    public void aliveMessage(LocalHostInfo gc) {
        globalController_= gc;
        System.out.println("Got alive message from global controller.");
        try {
            System.out.println("Sending to "+gc.getName()+":"+gc.getPort());
            gcInteractor_= new GlobalControllerInteractor(gc);
            gcInteractor_.respondToGlobalController(hostInfo_);
        } catch (java.net.UnknownHostException e) {
            System.err.println("Cannot contact global controller");
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch (java.io.IOException e) {
            System.err.println("Cannot contact global controller");
            System.err.println(e.getMessage());
            System.exit(-1);
        }catch (usr.interactor.MCRPException e) {
            System.err.println("Cannot contact global controller");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
       
    }
    
    /** 
     * Received start new router command
    */
    public boolean requestNewRouter (int routerId) 
    
    {

        Process child;

        String [] cmd= new String[3];
        cmd[0] = "/usr/bin/java";
        cmd[1] = "usr.router.Router";
        cmd[2] = String.valueOf(maxPort_);

        try {
            System.out.println(leadin() + "Starting Router on port "+maxPort_);

            child = new ProcessBuilder(cmd).start();

            String procName = "Router-" + maxPort_;
            childProcessWrappers_.put(procName, new ProcessWrapper(child, procName));

        } catch (java.io.IOException e) {
            System.err.println(leadin() + "Unable to execute command "+ Arrays.asList(cmd));
            System.err.println(e.getMessage());
            //System.exit(-1);
            return false;
        }

        /// In JVM Router
        ///Router router= new Router(maxPort_, "Router-" + routerId);
        ///router.start();
        ///routerList_.add(router);

        // Separate JVM Router
        // create a RouterInteractor
        // try 5 times, with 500 millisecond gap
        int tries = 0;
        int millis = 500;
        for (tries = 0; tries < 5; tries++) {
            // sleep a bit
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
            }

            // try and connect
            try {
                RouterInteractor interactor = new RouterInteractor("localhost", maxPort_);
                routerInteractors_.add(interactor);
                break;
            } catch (UnknownHostException uhe) {
            } catch (IOException e) {
            }
        }

        if (tries == 4) {
            // we didnt connect
            System.err.println(leadin() + "Unable to connect to Router on port " + maxPort_);
            return false;
        } else {
            // we connected
            BasicRouterInfo br= new BasicRouterInfo(routerId,0,hostInfo_,maxPort_);
            routers_.add(br);
            maxPort_+=2;
            return true;
        }
    }

    /**
     * Get the name of this LocalController.
     */
    public String getName() {
        return myName + ":" + hostInfo_.getPort();
    }

    /**
     * Get the ManagementConsole this ComponentController interacts with.
     */
    public ManagementConsole getManagementConsole() {
        return console_;
    }


    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String LC = "LC: ";

        return getName() + " " + LC;
    }



    
}



