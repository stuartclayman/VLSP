package usr.applications;

import usr.net.*;
import usr.logging.*;
import java.nio.ByteBuffer;
import usr.protocol.Protocol;

/**
 * An application for Ping 
 */
public class Ping implements Application {
    int gid_= 0;
    boolean running_= false;
    DatagramSocket socket_= null;

   
    
    /**
     * Do a ping
     */
    public Ping() {
    }

    /**
     * Initialisation for ping.
     */
    public ApplicationResponse init(String[] argv) throws NumberFormatException{

        if (argv.length != 1) {
            return new ApplicationResponse(false, leadin()+"PING COMMAND REQUIRES ROUTER ADDRESS AS ARGUMENT");
        }
        try {
            gid_= Integer.parseInt(argv[0]);
            return new ApplicationResponse(true, "");
        }
        catch (Exception e) {
            return new ApplicationResponse(false, leadin()+"PING COMMAND REQUIRES ROUTER ADDRESS");
        }
    }
    
    /** Start application with argument  */
    public ApplicationResponse start() {
        running_ = true;
        return new ApplicationResponse(true, "");
    }
    
    /** Implement graceful shut down */
    public ApplicationResponse stop() {
        running_= false;
        if (socket_ != null)
            socket_.close();

        Logger.getLogger("log").logln(USR.ERROR, "Ping stop");

        return new ApplicationResponse(true, "");
    }
    

    /** Run the ping application */
    public void run()  {
       
        try {
            socket_ = new DatagramSocket();
            
            GIDAddress dst= new GIDAddress(gid_); 
            // and we want to connect to port 0 (router fabric)
            socket_.connect(dst, 0);

            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket_.getLocalPort());

            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put("P".getBytes());
            Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
            if (socket_.send(datagram) == false) {
                throw new java.net.SocketException("socket_ returned false");
            }
        } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"Cannot open socket to write");
                Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());  
                return;
        }
        long now= System.currentTimeMillis();
        Datagram dg;
        while (running_) {
            
            dg = socket_.receive();
            if (dg == null) {
                
                Logger.getLogger("log").logln(USR.STDOUT, "Ping waiting");
                continue;
            } 
            Logger.getLogger("log").logln(USR.STDOUT, "Ping received in time: "+ (System.currentTimeMillis()- now) + " milliseconds ");
               
            return;
            
        }
        
        
    }

    String leadin() {
        String li= "PA: ";
        return li;
    }
}
