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
public class TCPNetIF implements NetIF , Runnable {
    //TODO: 
    // 1. implement more control packets
    // 2. implement state machine so NetIf can see the state of a Connection
    // 3. PAUSE a connection so it is connected but no traffic flows over it

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
    NetIFListener listener = null;

    long startTime = 0;
    long stopTime = 0;

    // closed ?
    boolean closed = true;

    // got a remote close 
    boolean remoteClose = false;

    // reading thread stuff
    Thread readThread = null;
    boolean paused = false;
    boolean reading = false;

    // Is the current reading thread running
    boolean running = false;

    // Inbound Thread
    InboundThread inboundThread = null;

    // Outbound Thread
    OutboundThread outboundThread = null;

    // a Queue of incoming Datagrams
    LinkedBlockingQueue<Datagram> incomingQueue;

    // a Queue of outgoing Datagram
    LinkedBlockingQueue<Datagram> outgoingQueue;

    // counts
    NetStats netStats;

    int MAX_QUEUE_SIZE = 4096;

    // queue high limit
    int QUEUE_PAUSE_LIMIT = MAX_QUEUE_SIZE * 4 / 5;
    // queue low limit
    int QUEUE_TOO_LOW = MAX_QUEUE_SIZE * 1 / 5;


    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointSrc src) throws IOException {
        connection = new ConnectionOverTCP(src);
        netStats = new NetStats();
        incomingQueue = new LinkedBlockingQueue<Datagram>(MAX_QUEUE_SIZE);    
        outgoingQueue = new LinkedBlockingQueue<Datagram>(MAX_QUEUE_SIZE);
    }

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointDst dst) throws IOException {
        connection = new ConnectionOverTCP(dst);
        netStats = new NetStats();
        incomingQueue = new LinkedBlockingQueue<Datagram>(MAX_QUEUE_SIZE);    
        outgoingQueue = new LinkedBlockingQueue<Datagram>(MAX_QUEUE_SIZE/2);
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
        if (listener != null) {
            Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: already has a NetIFListener");
        } else {
            listener = l;

            inboundThread = new InboundThread(listener, this, incomingQueue);
            inboundThread.start();

            outboundThread = new OutboundThread(connection, this, outgoingQueue);
            outboundThread.start();

        }

        return this;
    }

    /**
     * Send a Datagram -- sets source to this interface
     */
    public boolean sendDatagram(Datagram dg) {
        if (running == true) {
            // set the source address and port on the Datagram
            dg.setSrcAddress(connection.getAddress());

            return forwardDatagram(dg);
        } else {
            return false;
        }
    }
    
    /**
     * Forward a Datagram.
     */
    public boolean forwardDatagram(Datagram dg) {
        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " Forw(" + forwardCount + ")");

        netStats.setValue(NetStats.Stat.OutQueue, outgoingQueue.size());

        if (outgoingQueue.offer(dg)) {
            // Datagram went on queue
            if (outgoingQueue.size() > netStats.getValue(NetStats.Stat.BiggestOutQueue)) {
                netStats.setValue(NetStats.Stat.BiggestOutQueue, outgoingQueue.size());
                
            }
        } else {
            // its dropped
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "dropped at queue size " + outgoingQueue.size());
            netStats.increment(NetStats.Stat.OutDropped);
        }
            
        /*
        try {
            connection.sendDatagram(dg);
        } catch (Exception e) {
        }
        */

        return true;
    }

    
    /**
     * Close a NetIF
     */
    public synchronized void close() {
        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: " + getName() + " -> Close");

        if (closed) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin()+"Already closed");
            return;
        }

        // send a ControlMessage
        if (!remoteClose) {
            // if the close is initiated locally
            // send a control message to the other end
            //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: -> Close controlClose");

            controlClose();
        }

        reallyClose();
        
        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: " + getName() + " close() biggestIncomingSize = " + netStats.getValue(NetStats.Stat.BiggestInQueue) + " biggestOutgoingSize = " + netStats.getValue(NetStats.Stat.BiggestOutQueue));


    }

    /**
     * Really close the connection
     */
    private void reallyClose() {
        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: reallyClose " + getName() + " -> Close stop");

        // stop all threads
        stop();
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
    public synchronized NetStats getStats() {
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
     * and queues them up ready to be delivered to the NetIFListener.
     * If the queue is at the high limit then it waits until 
     * the queue has been drained to a certain level, the low limit,
     * and is told to start reading again.
     */
    public void run() {
        // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + readThread + " top of run()");

        // EOF
        boolean eof = false;

        // now go and read
        Datagram datagram = null;

	// sit in a loop and grab input
	while (running) {

            /*
            if (listener == null) {
                Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " NO listener");
                reallyClose();
            }
            */

            if (!eof) {
                if (incomingQueue.size() < QUEUE_PAUSE_LIMIT) {
                    // not EOF and there is room in the queue
                    // so read

                    try {
                        reading = true;

                        datagram = connection.readDatagram();

                        reading = false;
                    } catch (Exception ioe) {
                        Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF readDatagram error " + connection + " " + netStats.getValue(NetStats.Stat.InPackets) + " IOException " + ioe);
                        ioe.printStackTrace();
                        // TODO:  THIS ERROR DOES OCCUR SOMETIMES -- DO NOT KNOW WHY
                    }


                    // check the return value
                    if (datagram == null) {
                        // EOF
                        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF readDatagram NULL datagram");
                        eof = true;

                    } else {
                        // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " " + incomingCount + " got a Datagram");

                        incomingQueue.add(datagram);

                        // stats
                        netStats.increment(NetStats.Stat.InPackets);
                        netStats.add(NetStats.Stat.InBytes, datagram.getTotalLength());

                        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " Recv(" + incomingCount + ")");

                        netStats.setValue(NetStats.Stat.InQueue, incomingQueue.size());

                        if (incomingQueue.size() > netStats.getValue(NetStats.Stat.BiggestInQueue)) {
                            netStats.setValue(NetStats.Stat.BiggestInQueue, incomingQueue.size());
                        }

                        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: " + getName() + " Inf(" + incomingCount + ")");

                    }

                } else {
                    // if the queue has reached its limit
                    // dont read any more until we get out of holdOn()
                    // if (incomingQueue.size() >= QUEUE_PAUSE_LIMIT) {
                     holdOn(); // this sets paused
                }

            } else {
                // reached EOF
                // so wait for stop()
                waitFor();
            }
        }

        // the end
        theEnd();

    }

   
    /**
     * Wait for this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void waitFor() {
          try {
              Logger.getLogger("log").logln(USR.STDOUT, leadin()+"waiting");
              synchronized(this) {
                  wait();
              }
          } catch (InterruptedException ie) {
              //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"wait Interrupted");
          }
       
    }
    
    
    /**
     * Notify this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void theEnd() {
        synchronized(this) {
            notifyAll();
        }

    }

 



    /**
     * Wait a bit.
     */
    private synchronized void holdOn() {
        // the queue is actually too empty to wait()
        if (incomingQueue.size() < QUEUE_TOO_LOW) {
            // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: bail out of wait() at queue size: " + incomingQueue.size());
            return;
        }

        // Logger.getLogger("log").logln(USR.ERROR, "run() about to wait() at incomingQueue size: " + incomingQueue.size());

        // now sleep a bit
        try {
            paused = true;
            // WAS wait();
            Thread.sleep(50);
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
        // called after holdOn()

        // Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF:  informReadAgain " + readThread + " incomingQueue size: " + incomingQueue.size());
        notifyAll();
    }
    
    /**
     * Start the thread.
     */
    public void start() {
        if (running == false) {
            startTime = System.currentTimeMillis();

            readThread = new Thread(this, this.getClass().getName() + "-" + hashCode());
            running = true;
            readThread.start();
        }
    }

    /**
     * Stop the thread.
     */
    public void stop() {
        // this can cause the underlying connection to
        // close-on-interrupt if there is still data being written out.
        // we need to be careful

       // Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: in stop() with incoming queue size: " + incomingQueue.size() + " outgoing queue size = " + outgoingQueue.size());

        if (running == true) {
            try {
                running = false;

                boolean doJoin = false;

                // flush OutboundThread
                doJoin = outboundThread.isFinished();

                if (doJoin) {
                    //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: in join() outboundThread");
                    outboundThread.join(1000);  // Give thread time to finish
                }
  
                
                // flush InboundThread
                doJoin = inboundThread.isFinished();

                if (doJoin) {
                    //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: in join() inboundThread");
                    inboundThread.join(1000);   // Give thread time to finish
                }
                
                 // stop OutboundThread
                //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: terminate() outboundThread");
                outboundThread.terminate();
               

                // stop InboundThread
                //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: terminate() inboundThread");
                inboundThread.terminate();

                // stop reader
                //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: terminate() readThread");
                readThread.interrupt();

                // these 2 lines were in reallyClose
                //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: connection.close()");
                connection.close();


                stopTime = System.currentTimeMillis();

                closed = true;

            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: Exception in stop() " + e);
            }

            
        } else {
            //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: not running in stop() ");
            
        }

        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: exit stop() with incoming queue size: " + incomingQueue.size() + " outgoing queue size = " + outgoingQueue.size());
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
        //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: -> controlClose");
        ByteBuffer buffer = ByteBuffer.allocate(1);
        String c= "C";
        buffer.put(c.getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer); // WAS new IPV4Datagram(buffer);
         ByteBuffer b= ((DatagramPatch)datagram).toByteBuffer();


        try {
            boolean sent = forwardDatagram(datagram); // WAS connection.sendDatagram(datagram); 

            // stats
            netStats.increment(NetStats.Stat.OutPackets);
            netStats.add(NetStats.Stat.OutBytes, datagram.getTotalLength());

            return sent;

        } catch (Exception ioe) {
            Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF: controlClose error " + connection + " " + netStats.getValue(NetStats.Stat.OutPackets) + " IOException " + ioe);
            //ioe.printStackTrace();
            return true;
        }
 
    }
   

    /** Send routing table
    public boolean sendRoutingTable(byte[] table) {
        byte []toSend= new byte[table.length+1];
        
        toSend[0]= (byte)'T';
        System.arraycopy(table, 0, toSend,1,table.length);
        ByteBuffer buffer = ByteBuffer.allocate(table.length+1);
        buffer.put(toSend);
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer); // WAS new IPV4Datagram(buffer);


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
     * A thread class to deliver to the NetIFListener
     */
    class InboundThread extends Thread {
        // The NetIFListener - the RouterFabric
        NetIFListener listener;

        // The sending NetIF
        NetIF netIF;

        // The queue
        LinkedBlockingQueue<Datagram> queue;

        // is running
        boolean running = false;

        /**
         * Construct a InboundThread given a queue to take from
         * and a NetIFListener to send to.
         */
        public InboundThread(NetIFListener l, NetIF n, LinkedBlockingQueue<Datagram> q) {
            listener = l;
            netIF = n;
            queue = q;
            setName("InboundThread-" + n.getName());
        }

        public void run() {
            //Logger.getLogger("log").logln(USR.STDOUT, "InboundThread: run");

            running = true;

            Datagram datagram;
            int sleepCount = 0;

            while (running || queue.size() > 0) {
                try {
                    //Logger.getLogger("log").logln(USR.STDOUT, "InboundThread: queue size = " + queue.size());

                    if (listener.canAcceptDatagram(netIF)) {
                        // tell fabric we have a Datagram
                        datagram = queue.take();
                        listener.datagramArrived(netIF, datagram);
                        netStats.setValue(NetStats.Stat.InQueue, queue.size());
                        sleepCount = 0;
                    } else {
                        try {
                            sleepCount++;
                            //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF.InboundThread-" + netIF.getName() + " : SLEEP " + sleepCount + "incomingQueue size = " + queue.size());
                            Thread.sleep(50 * sleepCount);
                        } catch (InterruptedException ie) {
                            running = false;
                        }
                    }


                } catch (InterruptedException ie) {
                    // jumped out of queue.take()
                    running = false;
                    //Logger.getLogger("log").logln(USR.STDOUT, "InboundThread: interrupted");
                    break;
                }

            }
        }

        /**
         * Can we Stop the InboundThread
         */
        public boolean isFinished() {
            if (running == true) {
                running = false;

                try {
                    if (queue.size() > 0) {
                        // wait for queue to drain
                        //Logger.getLogger("log").logln(USR.STDOUT,leadin()+ "TCPNetIF.InboundThread: wait for queue to drain");
                        return true;
                    } else {
                        return false;
                    }

                } catch (Exception e) {
                    //Logger.getLogger("log").logln(USR.ERROR, "InboundThread: Exception in isFinished() " + e);
                    return false;
                }
            } else {
                return false;
            }
        }

        /**
         * Stop the InboundThread
         */
        public void terminate() {
                running = false;
                    
                try {
                    this.interrupt();
                } catch (Exception e) {
                    //Logger.getLogger("log").logln(USR.ERROR, "InboundThread: Exception in terminate() " + e);
                }

            
        }


    }


    /**
     * A thread class to send outbound traffic
     */
    class OutboundThread extends Thread {
        // The connection
        Connection connection;

        // The NetIF
        TCPNetIF netIF;

        // The queue
        LinkedBlockingQueue<Datagram> queue;

        // is running
        boolean running = false;

        /**
         * Construct a OutboundThread given a queue to take from
         * and a connection send to.
         */
        public OutboundThread(Connection c, TCPNetIF n, LinkedBlockingQueue<Datagram> q) {
            connection = c;
            netIF = n;
            queue = q;
            setName("OutboundThread-" + n.getName());
        }

        public void run() {
            //Logger.getLogger("log").logln(USR.STDOUT, "OutboundThread: run");

            running = true;

            Datagram datagram;

            while (running || queue.size() > 0) {
                try {
                    //Logger.getLogger("log").logln(USR.STDOUT, "OutboundThread: queue size = " + queue.size());


                    try {
                        datagram = queue.take();
                        netStats.setValue(NetStats.Stat.OutQueue, outgoingQueue.size());

                        boolean sent =  connection.sendDatagram(datagram);

                        // stats
                        netStats.increment(NetStats.Stat.OutPackets);
                        netStats.add(NetStats.Stat.OutBytes, datagram.getTotalLength());


                    } catch (IOException ioe) {
                        Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF.OutboundThread: send error " + connection + " " + netStats.getValue(NetStats.Stat.OutPackets) + " IOException " + ioe);
                        //ioe.printStackTrace();
                        Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF.OutboundThread: " + netIF.getName() + " -> ReallyClose from run()");
                        netIF.reallyClose();
                    }
                } catch (InterruptedException ie) {
                    // jumped out of queue.take()
                    running = false;
                    //Logger.getLogger("log").logln(USR.STDOUT, "TCPNetIF.OutboundThread: interrupted");
                    break;
                }
            }
        }


        /**
         * Stop the OutboundThread
         */
        public boolean isFinished() {
            if (running == true) {
                try {
                    running = false;

                    if (queue.size() > 0) {
                        // wait for queue to drain
                        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF.OutboundThread: wait for queue to drain");
                        return true;
                    } else {
                        return false;
                    }

                } catch (Exception e) {
                    //Logger.getLogger("log").logln(USR.ERROR, "OutboundThread: Exception in terminate() " + e);
                    return false;
                }
            } else {
                return false;
            }
        }


        /**
         * Stop the OutboundThread
         */
        public void terminate() {
                try {
                    running = false;
                    
                    this.interrupt();
                } catch (Exception e) {
                    //Logger.getLogger("log").logln(USR.ERROR, "OutboundThread: Exception in terminate() " + e);
                }
        }


    }

}
