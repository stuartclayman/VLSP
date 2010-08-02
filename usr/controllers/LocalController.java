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

class LocalController {
    private LocalHostInfo hostInfo_;
    private java.net.Socket clientSocket_;
    private LocalHostInfo globalController_;
    private Thread listenThread_;
    private ServerSocket serverSocket_= null;
    private Socket globalSocket_= null;
    private boolean listening_= true;
    private LocalControllerListener listener_= null;

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
    /** Received shut Down data gram from global */
    public void shutDown() {
        listening_= false;
        listener_.notListening();

        System.out.println("Got shutdown");

        
        try {
            if (!serverSocket_.isClosed()) {
                serverSocket_.close();
            }
        } catch (java.io.IOException e) {
            System.err.println("Error closing socket");
            System.exit(-1);
        }
    }
    
    /** Received alive message from global 
    */
    public void aliveMessage(LocalHostInfo gc) {
        globalController_= gc;
        System.out.println("Got keep alive");
        try {
            globalSocket_= new Socket(gc.getIp(), gc.getPort());
        } catch (java.io.IOException e) {
            System.err.println("Unable to open socket to global host");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        PayLoad dg= new PayLoad(PayLoad.ALIVE_MESSAGE,hostInfo_);
        dg.sendPayLoad(globalSocket_);
        System.out.println("Sent packet");
    }
    
    public LocalController (int port) {
        hostInfo_= new LocalHostInfo(port);
        startListening();
    }
    
    
    private void startListening() {
        
        try {
          serverSocket_ = new ServerSocket(hostInfo_.getPort());
        } catch (java.io.IOException e) {
          System.err.println("Could not listen on port:"+
            String.valueOf(hostInfo_.getPort()));
          System.exit(-1);
        }

        while (listening_) {
           
            try {
                //System.out.println("In accept loop");
                Socket skt= serverSocket_.accept();
                listener_= new LocalControllerListener(skt,this);
                //Thread th= new Thread(listener_);
                //th.start();
                listener_.listen();
            } catch (java.io.IOException e) {
                if (!listening_)
                    return;
            }
        }

    }
    

}

