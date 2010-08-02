
package usr.controllers;
import java.net.*;
import java.io.*;
import java.lang.*;



class LocalControllerListener implements Runnable {
    private LocalController localController_;
    private Socket socket_;
    private boolean listening_= true;
    private BufferedInputStream reader_;
    
    public LocalControllerListener(Socket socket, LocalController lc) {
        localController_= lc;
        socket_= socket;
        

    }
    
    public void run() {
        listen();
    }
    
    public void listen() {
        try {
            reader_ = new BufferedInputStream(socket_.getInputStream());
        } catch (java.io.IOException e) {
            System.err.println("Error creating buffered reader from socket!");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        while (listening_) {
            //System.out.println("In listen loop");
            try {
                getMessages();
            } catch (java.io.IOException e) {
                if (!listening_)
                    return;
                    
                System.err.println("Broken socket on this host!"+
                   e.getMessage());
                System.exit(-1);
                
            }
        }
        
        return;
    }
    
    public void notListening() {
        listening_= false;
    }
    
    private void getMessages() throws java.io.IOException {
        PayLoad readData= null;
        try {
            readData= new PayLoad(reader_);
        }
        catch (java.io.IOException e) {
            throw(e);
        }
        if (readData.getMessageType() == PayLoad.ALIVE_MESSAGE) {
            localController_.aliveMessage((LocalHostInfo)readData.getObject());
            return;
        }
        if (readData.getMessageType() == PayLoad.SHUTDOWN_MESSAGE) {
            localController_.shutDown();
            return;
        }
        
        System.err.println("Received unrecognisable datagram at listener"); 
        System.exit(-1);
    }
}
