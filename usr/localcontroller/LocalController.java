package usr.localcontroller;

import java.lang.*;
import java.util.*;
import java.net.*;
import usr.console.*;
import usr.router.*;
import usr.common.LocalHostInfo;
import usr.interactor.LocalControllerInteractor;

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
    private LocalControllerInteractor lcInteractor_= null;
    private LocalControllerManagementConsole console_= null;
    private boolean listening_ = true;
    private int maxPort_= 20000;  // Ports in use
    private ArrayList <BasicRouterInfo> routers_= null;
    private ArrayList <Router> routerList_= null;
    
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
        console_= new LocalControllerManagementConsole(this, port);
        console_.start();
    }
    
    /** Received shut Down data gram from global */
    public void shutDown() {

        System.out.println("Local controller got shutdown message from global controller.");
        System.out.println("Stopping all running routers"); // TODO send proper shut down
        for (int i= 0; i < routers_.size(); i++) {
            Router r= routerList_.get(i);
            r.stop();
        }
        console_.stop();
        System.exit(-1);
    }
    
    public LocalControllerInfo getHostInfo() {
        return hostInfo_;
    }
    
    /** Received alive message from global 
    */
    public void aliveMessage(LocalHostInfo gc) {
        globalController_= gc;
        System.out.println("Got alive message from global controller.");
        try {
            System.out.println("Sending to "+gc.getName()+":"+gc.getPort());
            lcInteractor_= new LocalControllerInteractor(gc);
            lcInteractor_.respondToGlobalController(hostInfo_);
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
    
    /** Received start new router command
    */
    
    public boolean requestNewRouter (int routerId) 
    
    {
        System.out.println("Starting Router on port "+maxPort_);
        Router router= new Router(maxPort_);
        router.start();
        routerList_.add(router);
        BasicRouterInfo br= new BasicRouterInfo(routerId,0,hostInfo_,maxPort_);
        routers_.add(br);
        maxPort_+=2;
        return true;
    }


    /**
     * Get the ManagementConsole this ComponentController interacts with.
     */
    public ManagementConsole getManagementConsole() {
        return console_;
    }

    
    
    
    

}

