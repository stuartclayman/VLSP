import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.*;

/** This class listens for messages sent to the global controller.
*/
class GlobalSocketController implements Runnable {
    private ServerSocket serverSocket_;
    private GlobalController globalController_;
    private BufferedInputStream reader_;
    boolean listening_= true;
    private ArrayList <GlobalControllerListener> listenerControllers_= null;
    private ArrayList <PayLoad> messageQueue_= null;   
        
    public GlobalSocketController(ServerSocket socket, GlobalController gc) {
        globalController_= gc;
        serverSocket_= socket;
        listenerControllers_= new ArrayList <GlobalControllerListener>();
        messageQueue_= new ArrayList <PayLoad>();
    }

    public synchronized void enque(PayLoad mess)
    { 
        messageQueue_.add(mess);
        globalController_.wakeUp();       
    }
    
    public synchronized PayLoad dequeue()
    {
        if (messageQueue_.size() == 0) {
            return null;
        }
        return messageQueue_.remove(0); 
    }
    
    public void run() {
        while (listening_) {
//            System.out.println ("In accept loop!");
            try {
                Socket sock= serverSocket_.accept();
                GlobalControllerListener gcl= 
                  new GlobalControllerListener(sock,this);
                Thread th= new Thread(gcl);
                th.start();
                listenerControllers_.add(gcl);
            } 
            catch (java.io.IOException e) {
                if (!listening_)
                    return;
                System.err.println("Server socket broken");
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
        return;
    }
    
    public void stopListening() {
        listening_= false;
        Iterator i= listenerControllers_.iterator();
        while (i.hasNext()) {
            GlobalControllerListener gcl= (GlobalControllerListener)i.next();
            gcl.stopListening();
        }
    }
    public void killListenerSockets() {
    
        Iterator i= listenerControllers_.iterator();
        while (i.hasNext()) {
            GlobalControllerListener gcl= (GlobalControllerListener)i.next();
            gcl.closeSocket();
        }
        try {
            serverSocket_.close();
        } catch (java.io.IOException e) {
            System.err.println ("Cannot close Server Socket:\n"+
              e.getMessage());
            System.exit(-1);
        }
    }
    

}
