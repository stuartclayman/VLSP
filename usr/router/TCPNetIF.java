package usr.router;

import usr.net.*;
import usr.logging.*;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
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
public class TCPNetIF implements NetIF , Runnable {
    // The connection
    ConnectionOverTCP connection;

    boolean theEnd= false;
    // The name of this 
    String name;

    // The weight
    int weight;

    // int ID
    int id;

    // Address at this end
    Address address;

    // Remote router name
    String remoteRouterName;

    // Remote Router Address
    Address remoteRouterAddress;

    // The Listener
    NetIFListener listener;

    // closed ?
    boolean closed = true;

    // got a remote close 
    boolean remoteClose = false;

    // Read Thread
    Thread readThread = null;
    boolean paused = false;
    boolean reading = false;

    // Thread doing queue.take()
    //Thread takeThread = null;
    boolean waitingForQueue = false;

    // Write Thread
    Thread writeThread = null;

    // Is the thread running
    boolean running = false;

    // a Queue of incoming Datagrams
//    BlockingQueue<Datagram> incomingQueue;

    // a Queue of outgoing Datagram
    //BlockingQueue<Datagram> outgoingQueue;

    // counts
    NetStats netStats;

    // queue high limit
    int QUEUE_PAUSE_LIMIT = 120;
    // queue low limit
    int QUEUE_TOO_LOW = 60;


   

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointSrc src) throws IOException {
        connection = new ConnectionOverTCP(src);
        netStats = new NetStats();
        // incomingQueue = new LinkedBlockingQueue<Datagram>();    
        // outgoingQueue = new LinkedBlockingQueue<Datagram>();
    }

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointDst dst) throws IOException {
        connection = new ConnectionOverTCP(dst);
        netStats = new NetStats();
        // incomingQueue = new LinkedBlockingQueue<Datagram>();    
        // outgoingQueue = new LinkedBlockingQueue<Datagram>();
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
    public NetIF setName(String name) {
        this.name = name;
        return this;
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
    public NetIF setWeight(int w) {
        weight = w;
        return this;
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
    public NetIF setID(int id) {
        this.id = id;
        return this;
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
    public NetIF setAddress(Address addr) {
        address = addr;
        connection.setAddress(addr);
        return this;
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
    public NetIF setRemoteRouterName(String name) {
        remoteRouterName = name;
        return this;
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
    public NetIF setRemoteRouterAddress(Address addr) {
        remoteRouterAddress = addr;
        return this;
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
    public NetIF setNetIFListener(NetIFListener l) {
        listener = l;

        // there is already something
       // if (incomingQueue.size() > 0) {
       //     listener.datagramArrived(this);
       // }
        return this;
    }

    /**
     * Send a Datagram -- sets source to this interface
     */
    public boolean sendDatagram(Datagram dg) {
         // set the source address and port on the Datagram
        dg.setSrcAddress(connection.getAddress());

        // stats
        netStats.increment(NetStats.Stat.OutPackets);
        netStats.add(NetStats.Stat.OutBytes, dg.getTotalLength());

        return connection.sendDatagram(dg);
    }
    
    /**
     * Forward a Datagram.
     */
    public boolean forwardDatagram(Datagram dg) {
        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " Forw(" + forwardCount + ")");

        //outgoingQueue.add(dg);
        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " " + forwardCount + " outgoingQueue size = " + outgoingQueue.size());

        // stats
        netStats.increment(NetStats.Stat.OutPackets);
        netStats.add(NetStats.Stat.OutBytes, dg.getTotalLength());
        
        connection.sendDatagram(dg);

        return true;
    }

    
    /**
     * Close a NetIF
     */
    public synchronized void close() {
        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: " + getName() + " -> Close");

        if (closed) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin()+"Aleard closed");
            return;
        }

        // send a ControlMessage
        if (!remoteClose) {
            // if the close is initiated locally
            // send a control message to the other end
            //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: -> Close controlClose");

            controlClose();
        }
        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: " + getName() + " -> Close stop");

        stop();

        connection.close();

        closed = true;
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
        return netStats;
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

    /*
     * Methods to do with reading using a thread.
     */

    /**
     * Thread body to read from Connection.
     * This reads the Datagrams from the network
     * and queues them up ready to be collected.
     * If the queue is at the high limit then it waits until 
     * the queue has been drained to a certain level, the low limit,
     * and is told to start reading again.
     * <p>
     * However, if the Datagram is a control Datagram, then it is 
     * processed immediately and not put on the queue.
     */
    public void run() {
        //TODO: 
        // 1. implement more control packets
        // 2. implement state machine so NetIf can see the state of a Connection
        // 3. PAUSE a connection so it is connected but no traffic flows over it

        // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + readThread + " top of run()");

	// sit in a loop and grab input
	while (running) {
            // if the queue has reached its limit
            // dont read any more until we get an interrupt
           // if (incomingQueue.size() >= QUEUE_PAUSE_LIMIT) {
            //    holdOn(); // this sets paused
           // }

            // now go and read
            reading = true;

            Datagram datagram = connection.readDatagram();

            reading = false;

            // check the return value
            if (datagram == null) {
                // EOF
                running = false;
            } else {
                // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " " + incomingCount + " got a Datagram");

                // stats
                netStats.increment(NetStats.Stat.InPackets);
                netStats.add(NetStats.Stat.InBytes, datagram.getTotalLength());

               // incomingQueue.add(datagram);

                    // inform the listener
                if (listener != null) {
                    //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " Recv(" + incomingCount + ")");

                    listener.datagramArrived(this, datagram);

                    //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " Inf(" + incomingCount + ")");
                } else {
                    // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " NO listener");
                }
            }
        }

        // the end
        theEnd();

    }

   
    
    /**
     * Notify this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void theEnd() {

    }

 



    /**
     * Wait a bit.
     */
    private synchronized void holdOn() {
        // the queue is actually too empty to wait()
      //  if (incomingQueue.size() < QUEUE_TOO_LOW) {
            // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: bail out of wait() at queue size: " + incomingQueue.size());
       //     return;
      //  }

        // Logger.getLogger("log").logln(USR.ERROR, "run() about to wait() at incomingQueue size: " + incomingQueue.size());

        // now wait
        try {
            paused = true;
            wait();
        } catch (InterruptedException ie) {
            paused = false;
            // Logger.getLogger("log").logln(USR.ERROR, "run() with Exception out of wait() at incomingQueue size: " + incomingQueue.size());
        }
        paused = false;

        // Logger.getLogger("log").logln(USR.ERROR, "run() out of wait() at incomingQueue size: " + incomingQueue.size());

    }

    /**
     * Notify read thread.
     */
    private synchronized void informReadAgain() {
        // This causes the wait() in run() to be woken up
        // and then real reading will start again
        // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF:  informReadAgain " + readThread + " incomingQueue size: " + incomingQueue.size());
        notifyAll();
    }
    
    /**
     * Start the thread.
     */
    public void start() {
        if (running == false) {
            readThread = new Thread(this, this.getClass().getName() + "-" + hashCode());
            running = true;
            readThread.start();

            //writeThread = new TCPNetIF.WriteThread((Connection)connection, outgoingQueue);
            //writeThread.start();
        }
    }

    /**
     * Stop the thread.
     */
    public void stop() {
        if (running == true) {
            try {
                running = false;
                readThread.interrupt();
                //writeThread.interrupt();


            } catch (Exception e) {
                // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: Exception in stop() " + e);
            }

            
        }
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
    protected boolean controlClose() {
        // Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: -> controlClose");
        ByteBuffer buffer = ByteBuffer.allocate(1);
        String c= "C";
        buffer.put(c.getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer); // WAS new IPV4Datagram(buffer);
         ByteBuffer b= ((DatagramPatch)datagram).toByteBuffer();

        // stats
        netStats.increment(NetStats.Stat.OutPackets);
        netStats.add(NetStats.Stat.OutBytes, datagram.getTotalLength());


        return connection.sendDatagram(datagram);
 
    }
   

    /** Send routing table
    public boolean sendRoutingTable(String table) {
        String toSend;
        toSend="T"+table;

        ByteBuffer buffer = ByteBuffer.allocate(toSend.length());
        buffer.put(toSend.getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer); // WAS new IPV4Datagram(buffer);
        //Logger.getLogger("log").logln(USR.ERROR, "SENDING ROUTING TABLE");
        //Logger.getLogger("log").logln(USR.ERROR, table);

        // stats
        netStats.increment(NetStats.Stat.OutPackets);
        netStats.add(NetStats.Stat.OutBytes, datagram.getTotalLength());

        return connection.sendDatagram(datagram);
        
    }
    */

    /**
     * A remote close was received.
     */
    public void remoteClose() {
        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: got remote close. stats = " + getStats()); 

        remoteClose = true;

        // we check the queue to see if it has any data.
        if (listener != null) {
                listener.netIFClosing(this);
        }

        
    }
    
    String leadin() {
      return "TCPNetIF "+name+":";
    }

    /**
     * A write thread class
     */
    public class WriteThread extends Thread {
        // The connection
        Connection connection;

        // The queue
        BlockingQueue<Datagram> queue;

        // is running
        boolean running = false;

        /**
         * Construct a WriteThread given a queue to read from
         * and a Connection to send to.
         */
        public WriteThread(Connection c, BlockingQueue<Datagram> q) {
            connection = c;
            queue = q;
        }

        public void run() {
            /*
            Logger.getLogger("log").logln(USR.STDOUT, "WriteThread: run");

            running = true;

            while (running) {
                Datagram datagram;
                try {
                    // Logger.getLogger("log").logln(USR.STDOUT, "WriteThread: queue size = " + queue.size());

                    datagram = outgoingQueue.take();

                    connection.sendDatagram(datagram);

                } catch (InterruptedException ie) {
                    running = false;
                    // Logger.getLogger("log").logln(USR.STDOUT, "WriteThread: interrupted");
                    break;
                }

            }
            */
        }


            
    }
}
