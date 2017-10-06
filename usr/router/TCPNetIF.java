package usr.router;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;

import usr.common.ANSI;
import usr.common.TimedThread;
import usr.common.TimedThreadGroup;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.ConnectionOverTCP;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramPatch;
import usr.net.TCPEndPointDst;
import usr.net.TCPEndPointSrc;
import usr.protocol.Protocol;

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
    int outq = 0;
    int weight;
    // int ID
    int id;
    boolean local_ = false;
    // Address at this end
    Address address;
    // Remote router name
    String remoteRouterName;
    // Remote Router Address
    Address remoteRouterAddress;
    // The Listener
    NetIFListener listener = null;
    // The RouterPort
    RouterPort port = null;

    long startTime = 0;
    long stopTime = 0;

    // closed ?
    Boolean closed = true;
    // got a remote close
    boolean remoteClose = false;

    // synchronization object
    Object closedSyncObj = new Object();
    // Is the current reading thread running
    boolean running_ = false;
    // Has the stream closed
    boolean eof = false;
    // Fabric device does the data transfer
    FabricDevice fabricDevice_ = null;
    // Run thread for main loop
    Thread runThread_ = null;

    Object runWait_ = null;
    Object runWaitSyncObj_ = new Object();
    boolean waiting_ = false;

    CountDownLatch latch = null;


    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointSrc src, NetIFListener l) throws IOException {
        connection = new ConnectionOverTCP(src);
        listener = l;
        runWait_ = new Object();
    }

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointDst dst, NetIFListener l) throws IOException {
        connection = new ConnectionOverTCP(dst);
        listener = l;
        runWait_ = new Object();
    }

    /**
     * Start the netIF  -- TODO make the queue lengths settable in router
     * control
     */
    public synchronized void start() {
        ThreadGroup group = new TimedThreadGroup("TCPNetIF-" + connection.getSocket().getPort());

        running_ = true;
        //System.err.println("New fabric device listener "+listener);
        fabricDevice_ = new FabricDevice(group, this, listener);
        fabricDevice_.setInQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
        fabricDevice_.setInQueueLength(100);
        fabricDevice_.setOutQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
        fabricDevice_.setOutQueueLength(100);
        fabricDevice_.setName(name);
        fabricDevice_.start();
        latch = new CountDownLatch(1);
        runThread_ = new TimedThread(group, this, "/" + name + "/TCPNetIF-" + connection.getSocket().getPort());
        runThread_.start();
    }

    /** Run method loops and grabs input from connection to queue in
        fabricDevice */
    @Override
    public void run() {
        Datagram datagram = null;

        while (running_) {
            if (eof) {
                synchronized (runWaitSyncObj_) {
                    try {
                        waiting_ = true;
                        runWaitSyncObj_.wait();
                        waiting_ = false;
                    } catch (InterruptedException e) {
                    }
                }
            }
            try {
                datagram = connection.readDatagram();
            } catch (Exception ioe) {
                // Probably EOF
                //Logger.getLogger("log").logln(USR.ERROR,
                //                              "TCPNetIF readDatagram error " + connection +
                //                              " IOException " + ioe);
                //ioe.printStackTrace();
            }

            if (datagram == null) {
                eof = true;
                break;
            }
            try {
                if (fabricDevice_.inIsBlocking()) {
                    fabricDevice_.blockingAddToInQueue(datagram, this);
                } else {
                    fabricDevice_.addToInQueue(datagram, this);
                }
            } catch (NoRouteToHostException e) {
            }
        }

        // reduce latch count by 1
        latch.countDown();

    }

    /**
     * Connect - phase1
     */
    @Override
    public boolean connectPhase1() throws IOException {
        //Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF " + connection.getEndPoint().getClass().getName() + " connect() " + connection.getEndPoint());

        boolean conn = connection.connect();

        closed = false;
        start();
        return conn;
    }

    /**
     * Connect - phase2
     */
    @Override
    public boolean connectPhase2() throws IOException {
        // For TCP everything is done in phase 1
        // Nothing to do here
        return true;
    }

    /**
     * Get the name of this NetIF.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the name of this NetIF.
     */
    @Override
    public void setName(String n) {
        name = n;

        if (fabricDevice_ != null) {
            fabricDevice_.setName(n);
        }

        if (runThread_ != null) {
            runThread_.setName("/" + n + "/TCPNetIF");
        }
    }

    /**
     * Get the weight of this NetIF.
     */
    @Override
    public int getWeight() {
        return weight;
    }

    /**
     * Set the weight of this NetIF.
     */
    @Override
    public void setWeight(int w) {
        Logger.getLogger("log").logln(USR.STDOUT,
                                      leadin() + ANSI.YELLOW + " TCPNetIF " + name + " set weight " + w + ANSI.RESET_COLOUR);
        weight = w;
    }

    /**
     * Get the ID of this NetIF.
     */
    @Override
    public int getID() {
        return id;
    }

    /**
     * Set the ID of this NetIF.
     */
    @Override
    public void setID(int id) {
        this.id = id;
    }

    /**
     * Get the Address for this connection.
     */
    @Override
    public Address getAddress() {
        return address; // WAS connection.getAddress();
    }

    /**
     * Set the Address for this connection.
     */
    @Override
    public void setAddress(Address addr) {
        address = addr;
        connection.setAddress(addr);
    }

    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    @Override
    public String getRemoteRouterName() {
        return remoteRouterName;
    }

    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    @Override
    public void setRemoteRouterName(String name) {
        remoteRouterName = name;

    }

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    @Override
    public Address getRemoteRouterAddress() {
        return remoteRouterAddress;
    }

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    @Override
    public void setRemoteRouterAddress(Address addr) {
        remoteRouterAddress = addr;
    }

    /**
     * Get the socket.
     */
    private Socket getSocket() {
        return connection.getSocket();
    }

    /**
     * Get the remote address to which this socket is connected.
     */
    public InetAddress getInetAddress() {
        return getSocket().getInetAddress();
    }

    /**
     * Gets the local address to which the socket is bound.
     */
    public InetAddress getLocalAddress() {
        return getSocket().getLocalAddress();
    }

    /**
     * Get the remote port number to which this socket is connected.
     */
    public int getPort() {
        return getSocket().getPort();
    }

    /**
     * Get the port number on the local host to which this socket is bound.
     */
    public int getLocalPort() {
        return getSocket().getLocalPort();
    }

    /**
     * Set the remote InetAddress and port
     */
    public void setRemoteAddress(InetAddress addr, int port) throws IOException {
        // For TCP there is nothing to do
    }

    /**
     * Get the Listener of this NetIF.
     */
    @Override
    public NetIFListener getNetIFListener() {
        return listener;
    }

    /**
     * Set the Listener of this NetIF.
     */
    @Override
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
     * Get the RouterPort a NetIF is plugIged into.
     */
    @Override
    public RouterPort getRouterPort() {
        return port;
    }

    /**
     * Set the RouterPort a NetIF is plugIged into.
     */
    @Override
    public void setRouterPort(RouterPort rp) {
        port = rp;
    }

    /**
     * Send a Datagram -- sets source to this interface and puts the datagram
       on the incoming queue for this interface
     */
    @Override
    public boolean sendDatagram(Datagram dg) throws NoRouteToHostException {
        if (running_ == true) {
            return enqueueDatagram(dg);
        } else {
            return false;
        }
    }

    /**
     * Puts a datagram on the incoming queue for this network interface
     */
    @Override
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException {
        return fabricDevice_.blockingAddToInQueue(dg, this);
    }

    /** Finally recv a datagram and send the datagram onwards */
    @Override
    public boolean recvDatagramFromDevice(Datagram dg, DatagramDevice dd) {
        boolean sent = false;
        try {
            if (dg.getSrcAddress() == null) {
                dg.setSrcAddress(getAddress());
            }

            sent = connection.sendDatagram(dg);

            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " NetIF " + name + " sent " + dg);
        } catch (ClosedByInterruptException cbie) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + " ClosedByInterruptException connection.send "+address+"->"+remoteRouterAddress);
            //cbie.printStackTrace();

            listener.closedDevice(this);
            return false;

        } catch (IOException e) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + " failure in connection.send "+address+"->"+remoteRouterAddress);
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());

            //e.printStackTrace();

            listener.closedDevice(this);
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
    @Override
    public void remoteClose() {
        if (closed) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Already closed when remoteClose() called");
            return;
        }
        Logger.getLogger("log").logln(USR.STDOUT, leadin() +" RemoteClose");
        remoteClose = true;


        CloseThread ct = new CloseThread(this, this.closed);
        Thread t = new TimedThread(ct, "RemoteClose-"+name);
        t.start();
    }

    /**
     * Close a NetIF -- must be synchronized to prevent close() exiting prematurely when a
     * remoteClose has already been encountered -- close will never exit until the netif actually
     * is closed -- synchronized on "closed" object to prevent lock ups with other sync objects
     */
    @Override
    public void close() {
        synchronized (closedSyncObj) { // prevent this running twice by blocking
            Logger.getLogger("log").logln(USR.STDOUT, leadin() +" Close");

            if (closed) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Already closed when close() called");
                return;
            }
            closed = true;

            // send a ControlMessage if this is a local close
            // and not a remote close
            if (!remoteClose) {
                controlClose();
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Did control close");
            }


            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" About to stop fabricDevice");

            // tell the fabricDevice to stop
            if (fabricDevice_ != null) {
                fabricDevice_.stop();
            }

            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Did stop fabricDevice");

            running_ = false;

            // close the connection
            if (!remoteClose) {
                connection.close();
            }

            runThread_.interrupt();

            // join runThread when it is  finished
            try {
                latch.await();
            } catch (InterruptedException ie) {
            }

            // notify the run()
            if (waiting_) {
                synchronized (runWaitSyncObj_) {
                    runWaitSyncObj_.notify();
                }
            }
        }
    }

    /**
     * Is closed.
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Consturct and send a control message.
     */
    protected boolean controlClose() {
        synchronized (closedSyncObj) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Sending controlClose to "+remoteRouterAddress);
            ByteBuffer buffer = ByteBuffer.allocate(1);
            String c = "C";
            buffer.put(c.getBytes());
            Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
            ((DatagramPatch)datagram).toByteBuffer();
            datagram.setDstAddress(remoteRouterAddress);
            try {
                sendDatagram(datagram);
                return true;

            } catch (Exception ioe) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+" controlClose error " + connection + " IOException " + ioe);
                //ioe.printStackTrace();
                return true;
            }
        }
    }

    /**
     * Get the interface stats.
     * Returns a NetStats object.
     */
    @Override
    public NetStats getStats() {
        return fabricDevice_.getNetStats();
    }

    /** Accessor function for the fabric device associated with this */
    @Override
    public FabricDevice getFabricDevice() {
        return fabricDevice_;
    }

    /**
     * To String
     */
    @Override
    public String toString() {
        Address address = getAddress();
        Address remoteAddress = getRemoteRouterAddress();

        String ifName = getRouterPort() == null ? ("No port") : ("if" + Integer.toString(getRouterPort().getPortNo()));

        return ifName + " W(" + getWeight() + ") = " +
               (address == null ? "No_Address" : "" + address) + " => " +
               //getRemoteRouterName() + " " +
               (remoteAddress == null ? "No_Remote_Address" : "" + remoteAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NetIF) {
            NetIF b = (NetIF)obj;
            return getName().equals(b.getName());
        } else {
            return false;
        }

    }

    /**
     * hashcode
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }    

    @Override
    public boolean isLocal() {
        return local_;
    }

    public void setLocal(boolean l) {
        local_ = l;
    }

    String leadin() {
        return "TCPNetIF "+name+":";
    }

    /** Thread to perform remote close on netif */
    class CloseThread implements Runnable {
        NetIF netif_;
        CloseThread(NetIF n, Boolean closed) {
            netif_ = n;
        }

        @Override
        public void run() {
            // sclayman synchronized (closed) {
            netif_.close();
            //}
        }

    }

}
