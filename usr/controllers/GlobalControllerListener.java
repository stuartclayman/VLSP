package usr.controllers;
import java.net.*;
import java.io.*;
import java.lang.*;

/** This class listens for messages sent to the global controller.
*/
class GlobalControllerListener implements Runnable {
    private Socket socket_;
    private LocalHostInfo localHost_= null;
    private GlobalSocketController globalController_;
    private BufferedInputStream reader_;
    boolean listening_= true;
        
    public GlobalControllerListener(Socket socket, GlobalSocketController gc) {
        socket_= socket;
        globalController_= gc;
    }
    
    public void run() {
        listen();
    }
    
    public void listen() {
//        System.out.println("Spawned listener");
        try {
            reader_ = new BufferedInputStream(socket_.getInputStream());
        } catch (java.io.IOException e) {
            System.err.println("Error creating buffered reader from socket!");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        while (listening_) {
//            System.out.println("In listen loop");
            try {
                getMessages();
            } catch (java.io.IOException e) {
                if (!listening_)
                    return;
                    
                System.err.println("Broken socket on this host!\n"+
                   e.getMessage());
                System.exit(-1);
                
            }
        }
        
        return;
    }
    
    
    
    public void stopListening() {
        listening_= false;
    }
    
    public void closeSocket() {
        try {
            socket_.close();
        } catch (java.io.IOException e) {
            System.err.println ("Listener cannot close socket:\n"+
              e.getMessage());
            System.exit(-1);
        }
    }
    
    private void getMessages() throws java.io.IOException {
        PayLoad readData= null;
//        System.out.println("getMessages");
        try {
            readData= new PayLoad(reader_);
        }
        catch (java.io.IOException e) {
            throw(e);
        }
        globalController_.enque(readData);
    }
}
