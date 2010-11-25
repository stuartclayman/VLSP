package usr.router;

import usr.net.*;
import usr.logging.*;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A Network Interface for a Router using TCP
 * <p>
 * On the reading side, it queues up Datagrams that come
 * from the remote end.
 * <p>
 * It also implements control datagrams, so one end can inform the
 * other end of stuff.
 */
public class TCPNetIF implements NetIF, Runnable {

    // The connection
    ConnectionOverTCP connection;
    // The name of this 
    String name;
    // The weight
    int outq= 0;
    int weight;
    // int ID
    int id;
    boolean local_= false;
    // Address at this end
    Address address;
    // Remote router name
    String remoteRouterName;
    // Remote Router Address
    Address remoteRouterAddress;
    // The Listener
    NetIFListener listener = null;

    long startTime = 0;
    long stopTime = 0;

    // closed ?
    boolean closed = true;
    // got a remote close 
    boolean remoteClose = false;
    // Is the current reading thread running
    boolean running_ = false;
    // Has the stream closed
    boolean eof= false;
    
    FabricDevice fabricDevice_= null;

    Thread runThread_= null;



    Object runWait_= null;

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointSrc src, NetIFListener l) throws IOException {
        connection = new ConnectionOverTCP(src);
        listener= l;
        
    }

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointDst dst, NetIFListener l) throws IOException {
        connection = new ConnectionOverTCP(dst);
        listener= l;
    }
    
      Datagram datagram = null;

    /** Run method loops and grabs input from connection to queue in
       fabricDevice */
 	  public void run() 
	  {
      	while (running_) {
      	    if (eof) {
      	        runWait_= new Object();
      	        synchronized (runWait_) {
      	            try {
      	                runWait_.wait();
      	            } catch (InterruptedException e) {
      	            }
      	        }
      	    }
            try { 
                datagram = connection.readDatagram();
            } catch (Exception ioe) {
                Logger.getLogger("log").logln(USR.ERROR, 
                "TCPNetIF readDatagram error " + connection + 
                " IOException " + ioe);
                ioe.printStackTrace();
            } 
            if (datagram == null) {
                eof = true;
                continue;
            }
            try {
                //fabricDevice_.blockingAddToInQueue(datagram, this);
                fabricDevice_.addToInQueue(datagram,this);
            } catch (NoRouteToHostException e) {
            }
        }

    }

    /**
     * Activate
     */
    public boolean connect() throws IOException {
        boolean conn = connection.connect();

        closed = false;
        start();
        return conn;
    }



    /**
     * Get the name of this NetIF.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this NetIF.
     */
    public void setName(String n) {
        name = n;
        if (fabricDevice_ != null) {
            fabricDevice_.setName(n);
        }
        if (runThread_ != null) {
            runThread_.setName("TCPNetIF-"+n);
        }
    }

    /**
     * Get the weight of this NetIF.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Set the weight of this NetIF.
     */
    public void setWeight(int w) {
        weight = w;
    }

    /**
     * Get the ID of this NetIF.
     */
    public int getID() {
        return id;
    }

    /**
     * Set the ID of this NetIF.
     */
    public void setID(int id) {
        this.id = id;
    }


    /**
     * Get the Address for this connection.
     */
    public Address getAddress() {
        return address; // WAS connection.getAddress();
    }

    
    /**
     * Set the Address for this connection.
     */
    public void setAddress(Address addr) {
        address = addr;
        connection.setAddress(addr);
    }


    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    public String getRemoteRouterName() {
        return remoteRouterName;
    }


    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterName(String name) {
        remoteRouterName = name;

    }

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    public Address getRemoteRouterAddress() {
        return remoteRouterAddress;
    }

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterAddress(Address addr) {
        remoteRouterAddress = addr;
    }


    /**
     * Get the socket.
     */
   Socket getSocket() {
         return connection.getSocket();
    }

    /**
     * Get the Listener of this NetIF.
     */
    public NetIFListener getNetIFListener() {
        return listener;
    }

    /**
     * Set the Listener of this NetIF.
     */
    public void setNetIFListener(NetIFListener l) {
        if (listener != null) {
            Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: already has a NetIFListener");
        } else {
            listener = l;
            if (fabricDevice_ != null) {
                fabricDevice_.setListener(l);
            }
        }
    }

    /**
     * Send a Datagram -- sets source to this interface and puts the datagram
     on the incoming queue for this interface
     */
    public synchronized boolean sendDatagram(Datagram dg) throws NoRouteToHostException{
        if (running_ == true) {
            // set the source address and port on the Datagram
            dg.setSrcAddress(getAddress());
            return enqueueDatagram(dg);
        } else {
            return false;
        }
    }
    
    /**
     * Puts a datagram on the incoming queue for this network interface
     */
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException {
        return fabricDevice_.blockingAddToInQueue(dg, this);
    }

  
    /** Finally send the datagram onwards */
    
    public boolean outQueueHandler(Datagram dg, DatagramDevice dd) {
        boolean sent= false;
        try {
            sent= connection.sendDatagram(dg);
        } catch (IOException e) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + " failure in connection.send "+address+"->"+remoteRouterAddress);
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + e.getMessage());
            return false;
        }
        if (sent == false) {
            listener.closedDevice(this);
            return false;
        }
        return true;
    }
    
    /** Close a netIF given remote end has called close -- this is done as a
    spawned process since it would otherwise block out queues which might need
    to be written to during close*/
    public synchronized void remoteClose() {
        if (closed) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Already closed when remoteClose() called");
            return;
        }
        remoteClose= true;
        CloseThread ct= new CloseThread(this);
        Thread t= new Thread(ct,"RemoteClose-"+name);
        t.start();
    }
    
    /**
     * Close a NetIF -- must be synchronized to prevent close() exiting prematurely when a
     * remoteClose has already been encountered -- close will never exit until the netif actually
     * is closed
     */
    public synchronized void close() {
        Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: " + getName() + " -> Close");
        
        if (closed) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Already closed when close() called");
            return;
        }
        closed= true;
        // send a ControlMessage
        if (!remoteClose) {
            controlClose();
        }
        if (fabricDevice_ != null) 
            fabricDevice_.stop();
        running_= false;
        connection.close();
        runThread_.interrupt();
        try {
            runThread_.join();
        } catch (InterruptedException e) {
        
        }
        if (runWait_ != null) {
            synchronized(runWait_) {
                runWait_.notify();
            }
        }
    }


    /**
     * Is closed.
     */
    public synchronized boolean isClosed() {
        return closed;
    }
        
    /**
     * Get the interface stats.
     * Returns a NetStats object.
     */
    public NetStats getStats() {
        return fabricDevice_.getNetStats();
    }
    
    /** Accessor function for the fabric device associated with this */
    public FabricDevice getFabricDevice() {
        return fabricDevice_;
    }

    /**
     * To String
     */
    public String toString() {
        Address address = getAddress();
        Address remoteAddress = getRemoteRouterAddress();

        return getName() + " W(" + getWeight() + ") = " +
            (address == null ? "No_Address" : "" + address) +
            " => " + getRemoteRouterName() + " " +
            (remoteAddress == null ? "No_Remote_Address" : "" + remoteAddress);
    }
    
    /**
     * Start the netIF  -- TODO make the queue lengths settable in router
     * control
     */
    public synchronized void start() {
        running_= true;
        //System.err.println("New fabric device listener "+listener);
        fabricDevice_= new FabricDevice(this, listener); 
        fabricDevice_.setInQueueDiscipline(FabricDevice.QUEUE_DROPPING);
        fabricDevice_.setInQueueLength(100);
        fabricDevice_.setOutQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
        fabricDevice_.setOutQueueLength(100);
        fabricDevice_.setName(name);
        fabricDevice_.start();
        runThread_= new Thread(this,"TCPNetIF-"+name);
        runThread_.start();
    }



    public boolean equals(Object obj) { 
        if (obj instanceof NetIF) {
            NetIF b = (NetIF)obj;
            return getName().equals(b.getName());
        } else {            
            return false;
        }

    }


    /**
     * Consturct and send a control message.
     */
    protected synchronized boolean controlClose() {
        Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: -> controlClose");
        ByteBuffer buffer = ByteBuffer.allocate(1);
        String c= "C";
        buffer.put(c.getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer); // WAS new IPV4Datagram(buffer);
         ByteBuffer b= ((DatagramPatch)datagram).toByteBuffer();
        datagram.setDstAddress(remoteRouterAddress);
        
        ByteBuffer buffer2 = ByteBuffer.allocate(0);
        Datagram datagram2 = DatagramFactory.newDatagram(Protocol.SHUTCONNECTION,buffer2);
        datagram2.setDstAddress(remoteRouterAddress);
        try {
            
            sendDatagram(datagram); 


            sendDatagram(datagram2); 
            return true;

        } catch (Exception ioe) {
            Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: controlClose error " + connection + " IOException " + ioe);
            //ioe.printStackTrace();
            return true;
        }
 
    }
   
    public boolean isLocal() {
        return local_;
    }
    
    public void setLocal(boolean l) {
        local_= l;
    }
    
    String leadin() {
      return "TCPNetIF "+name+":";
    }
    
    /** Thread to perform remote close on netif */
    class CloseThread implements Runnable {
        TCPNetIF netif_;
        CloseThread(TCPNetIF n) {
            netif_= n;
        }
        
        public void run() {
            netif_.close();
        }
    }

}
