package usr.applications;
import usr.router.*;
import usr.net.*;
import java.nio.ByteBuffer;
import usr.protocol.*;

/**
 * An application for Ping 
 */
public class PingApplication implements Application {
    int gid_= 0;
    Router router_= null;
    boolean running_= false;
    AppSocket socket_= null;
    Thread myThread_;
   
    
    public PingApplication(Router r, String args) throws NumberFormatException{
        router_= r;
        String [] argv= args.split(" ");
        if (argv.length != 1) {
             throw new NumberFormatException(leadin()+"PING COMMAND REQUIRES ROUTER GID AS ARGUMENT");
        }
        try {
            gid_= Integer.parseInt(argv[0]);
        }
        catch (Exception e) {
            throw new NumberFormatException(leadin()+"PING COMMAND REQUIRES ROUTER GID");
        }
    }
    
    /** Run the ping application */
    public synchronized void run()  {
       
        AppSocket socket_= null;
        
        try {
            socket_ = new AppSocket(router_);
            
            GIDAddress dst= new GIDAddress(gid_); 
            // and we want to connect to port 0 (router fabric)
            socket_.connect(dst, 0);
            System.err.println("Socket has source port "+socket_.getLocalPort());
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put("P".getBytes());
            Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
            if (socket_.send(datagram) == false) {
                throw new java.net.SocketException("socket_ returned false");
            }
        } catch (Exception e) {
                System.err.println(leadin()+"Cannot open socket to write");
                System.err.println(leadin()+e.getMessage());  
                return;
        }
        long now= System.currentTimeMillis();
        Datagram dg;
        while (running_) {
            
            dg = socket_.receive();
            if (dg == null) {
                
                System.out.println("Ping waiting");
                continue;
            } 
            System.out.println("Ping received in time: "+ (System.currentTimeMillis()- now) + " milliseconds ");
               
            stop();
            return;
            
        }
        
        
    }
    
    /** Exit the application */
    void cleanUp() 
    {
        router_.commandExit(this);
    }
    
    
    
    /** Start application with argument  */
    public boolean start() {
        myThread_ = new Thread(this);
        running_ = true;
        myThread_.start();
        return true;
    }
    
    /** Implement graceful shut down */
    public synchronized boolean stop() {
        running_= false;
        if (socket_ != null)
            socket_.close();
        myThread_.interrupt();
        cleanUp();
        return true;
    }
    
    /** Implement ungraceful shut down */
    public void exit(int code)
    {
        System.err.println(leadin()+" exiting with error "+code);
        System.exit(code);
    }
    


    String leadin() {
        String li= "PA: ";
        return li;
    }
}
