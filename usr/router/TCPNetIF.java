package usr.router;

import usr.net.*;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Map;
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
    Thread takeThread = null;
    boolean waitingForQueue = false;


    // Is the thread running
    boolean running = false;

    // a Queue of incoming Datagrams
    BlockingQueue<Datagram> queue;

    // queue high limit
    int QUEUE_PAUSE_LIMIT = 120;
    // queue low limit
    int QUEUE_TOO_LOW = 60;


   

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointSrc src) throws IOException {
        connection = new ConnectionOverTCP(src);
        queue = new LinkedBlockingQueue<Datagram>();    
    }

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointDst dst) throws IOException {
        connection = new ConnectionOverTCP(dst);
        queue = new LinkedBlockingQueue<Datagram>();    
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
        return this;
    }

    /**
     * Send a Datagram.
     */
    public boolean sendDatagram(Datagram dg) {
        return connection.sendDatagram(dg);
    }

    public boolean equals(NetIF b) 
    { 
        if (b == null)
            return false;
        return getName().equals(b.getName());
    }

    /**
     * Read a Datagram.
     */
    public Datagram readDatagram() {
        // Get a Datagram from queue while queue has something in it
        // Only grab data if we are running or if there is residual 
        // stuff in the queue

        // which thread is doing the queue take()
        takeThread = Thread.currentThread();


        while (true) {  // loop until we get a Datagram or null
                        // this usually happens after 1 go, but
                        // it is possible to get an interrupt

            if ((running && queue.size() >= 0) || (!running && queue.size() > 0)) {
                // return a Datagram
                if (remoteClose && queue.size() == 0) {
                    // we need to close 
                    // so tell the Fabric
                    if (listener != null) {
                        listener.netIFClosing(this);
                    }

                    return null;
                } else {

                    try {
                        // if the reader is paused and
                        // the queue is empty 
                        // start reading again
                        if (queue.size() == QUEUE_TOO_LOW) {
                            informReadAgain();
                        }

                        waitingForQueue = true;

                        Datagram datagram = queue.take();

                        waitingForQueue = false;

                        return datagram;

                    } catch (InterruptedException ie) {
                        // System.err.println("TCPNetIF: readDatagram() interrupt");
                        waitingForQueue = false;
                        //return null;
                        continue;
                    }
                }
            } else {
                // System.err.println("TCPNetIF: readDatagram() return null. running = " + running + " queue.size() == " + queue.size());
                return null;
            }
        }
    }

    
    /**
     * Close a NetIF
     */
    public synchronized void close() {
        if (closed) {
            return;
        }

        // send a ControlMessage
        if (!remoteClose) {
            // if the close is initiated locally
            // send a control message to the other end
            controlClose();
        }

        stop();

        try {
            readThread.join();
        } catch (InterruptedException ie) {
            // System.err.println("TCPNetIF: close - InterruptedException for readThread join on " + connection);
        }
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
     * A map of values like:
     * "in_bytes" -> in_bytes
     * "in_packets" -> in_packets
     * "in_errors" -> in_errors
     * "in_dropped" -> in_dropped
     * "out_bytes" -> out_bytes
     * "out_packets" -> out_packets
     * "out_errors" -> out_errors
     * "out_dropped" -> out_dropped
     */
    public Map<String, Number> getStats() {
        return null;
    }

    /**
     * To String
     */
    public String toString() {
        return "TCPNetIF: " + getName() + " @ " + connection.toString();
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

        // System.err.println("TCPNetIF: " + readThread + " top of run()");

	// sit in a loop and grab input
	while (running) {
            // if the queue has reached its limit
            // dont read any more until we get an interrupt
            if (queue.size() >= QUEUE_PAUSE_LIMIT) {
                holdOn(); // this sets paused
            }

            // now go and read
            reading = true;

            Datagram datagram = connection.readDatagram();

            reading = false;

            // check the return value
            if (datagram == null) {
                // EOF
                running = false;
            } else {
                queue.add(datagram);

                    // inform the listener
                if (listener != null) {
                    listener.datagramArrived(this);
                }
            }
        }

        // the end
        theEnd();

    }

    /**
     * Notify main thread.
     */
    private void theEnd() {
        if (waitingForQueue) {
            // System.err.println("TCPNetIF:  theEnd interrupt " + takeThread);
            takeThread.interrupt();
        }
    }

    /**
     * Wait a bit.
     */
    private synchronized void holdOn() {
        // the queue is actually too empty to wait()
        if (queue.size() < QUEUE_TOO_LOW) {
            //System.err.println("TCPNetIF: bail out of wait() at queue size: " + queue.size());
            return;
        }

        // System.err.println("run() about to wait() at queue size: " + queue.size());

        // now wait
        try {
            paused = true;
            wait();
        } catch (InterruptedException ie) {
            paused = false;
            // System.err.println("run() with Exception out of wait() at queue size: " + queue.size());
        }
        paused = false;

        // System.err.println("run() out of wait() at queue size: " + queue.size());

    }

    /**
     * Notify read thread.
     */
    private synchronized void informReadAgain() {
        // This causes the wait() in run() to be woken up
        // and then real reading will start again
        // System.err.println("TCPNetIF:  informReadAgain " + readThread + " queue size: " + queue.size());
        notify();
    }
    
    /**
     * Start the thread.
     */
    public void start() {
        if (running == false) {
            readThread = new Thread(this, this.getClass().getName() + "-" + hashCode());
            running = true;
            readThread.start();
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
            } catch (Exception e) {
                // System.err.println("TCPNetIF: Exception in stop() " + e);
            }
        }
    }


    public void setRemoteClose(boolean rc) {
        remoteClose= rc;
    }   

    /**
     * Consturct and send a control message.
     */
    protected boolean controlClose() {
        // System.out.println("TCPNetIF: -> controlClose");
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("C".getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer); // WAS new IPV4Datagram(buffer);

        return connection.sendDatagram(datagram);
 
    }


    /** Send routing table -- require one in response if necessary */
    public boolean sendRoutingTable(String table, boolean requireResponse) {
        String toSend;
        if (requireResponse) {
            toSend="R"+table;
        } else {
            toSend="T"+table;
        }
        ByteBuffer buffer = ByteBuffer.allocate(toSend.length());
        buffer.put(toSend.getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer); // WAS new IPV4Datagram(buffer);
        //System.err.println("SENDING ROUTING TABLE");
        //System.err.println(table);
        return connection.sendDatagram(datagram);
        
    }
    
    /**
     * A remote close was received.
     */
    public void remoteClose() {
        // System.err.println("TCPNetIF: got remote close"); 

        // we check the queue to see if it has any data.
        if (queue.size() == 0) {
            // there is nothing in the queue
            // so close immediately
            // by telling the Fabric
            if (listener != null) {
                listener.netIFClosing(this);
            }

        } else {
            // remoteClose is true
            // so we will close when the queue size gets to 0 
            // in readDatagram.
        }
    }


}
