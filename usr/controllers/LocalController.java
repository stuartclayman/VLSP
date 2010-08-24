/** The local controller is intended to run on every machine.  
 * Its job is to start up router processes as needed.
 * It should be started by being given a port to listen on
 * specified on the command line
 * java LocalController.java 8080
 * In its own start up the Global Controller will contact each
 * Local Controller to give it more state
**/

package usr.controllers;
import java.lang.*;
import java.util.*;
import java.net.*;
import usr.interactor.*;

import usr.common.LocalHostInfo;

public class LocalController {
    private LocalHostInfo hostInfo_;
    private LocalHostInfo globalController_;
    private Thread listenThread_;
    private LocalControllerInteractor lcInteractor_= null;
    private LocalControllerManagementConsole console_= null;
    private boolean listening_ = true;

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
        hostInfo_= new LocalHostInfo(port);
        console_= new LocalControllerManagementConsole(this, port);
        console_.start();
    }
    
    /** Received shut Down data gram from global */
    public void shutDown() {

        System.out.println("Got shutdown");
        System.exit(-1);
    }
    
    /** Received alive message from global 
    */
    public void aliveMessage(LocalHostInfo gc) {
        globalController_= gc;
        System.out.println("Got keep alive");
        try {
            System.out.println("Sending to "+gc.getName()+":"+gc.getPort());
            lcInteractor_= new LocalControllerInteractor(gc);
            lcInteractor_.respondToGlobalController(gc);
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
    
    
    
    
    

}

