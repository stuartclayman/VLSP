package usr.router;

import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.ClosedByInterruptException;
import usr.net.Datagram;
import usr.net.SocketAddress;


/**
 * An AppSocket acts like a socket, and talks to the
 * AppSocketMux in order to get Datagrams in and out
 * of applications and into the Router.
 */
public class AppSocket {
    // The AppSocketMux this socket talks to
    AppSocketMux appSockMux;

    // the local Address
    Address localAddress;

    // the local port this is listening on
    int localPort;

    // the Address of remote end
    Address remoteAddress;

    // the port of the remote end
    int remotePort;

    // is it bound
    boolean isBound = false;

    // is it connected
    boolean isConnected = false;

    // is it closed
    boolean isClosed = false;

    // The queue take thread
    Thread takeThread;

    // The queue to take from
    LinkedBlockingQueue<Datagram> queue;

    /**
     * Constructs an AppSocket and binds it to any available port
     * on the local Router. The socket will be bound to the wildcard address,
     * an IP address chosen by the Router.
     */
    public AppSocket(Router r) throws SocketException {
        appSockMux = r.getAppSocketMux();

        // find the next free port number for the local end
        int freePort = appSockMux.findNextFreePort();

        bind(freePort);
    }

    /**
     * Construct an AppSocket attached to the specified Router.
     * It binds to the local port.
     */
    public AppSocket(Router r, int port) throws SocketException {
        appSockMux = r.getAppSocketMux();

        bind(port);
    }

    /**
     * Construct an AppSocket attached to the specified Router.
     * It connects to the Socket at the remote address and remote port.
     *  When a socket is connected to a remote address, packets may only
     * be sent to or received from that address.
     */
    public AppSocket(Router r, Address addr, int port) throws SocketException {
        appSockMux = r.getAppSocketMux();

        // find the next free port number for the local end
        int freePort = appSockMux.findNextFreePort();

        bind(freePort);

        connect(addr, port);
    }

    /**
     * Get the AppSockMux this talks to.
     */
    AppSocketMux getAppSocketMux() {
        return appSockMux;
    }

    /**
     * Binds this DatagramSocket to a port.
     */
    public void bind(int port) throws SocketException {
        if (isBound | isConnected) {
            //throw new SocketException("Cannot bind a socket already " +
            //                          (isBound ? "bound" : (isConnected ? "connected" : "setup")));
            appSockMux.removeAppSocket(this);
        }

        // check with the AppSocketMux if a socket can listen
        // on the specified port
        if (appSockMux.isPortAvailable(port)) {
            // the port is free
            localPort = port;
            isBound = true;
            isClosed = false;

            // Logger.getLogger("log").logln(USR.ERROR, "AppSocket: bound to port " + localPort);

            // register with AppSocketMux
            appSockMux.addAppSocket(this);
        } else {
            throw new SocketException("Port not free: " + port);
        }

        // this was in recieve()
        // but don;t need it on every call
        queue = appSockMux.getQueueForPort(localPort);


    }

    /**
     * Connects the socket to a remote address for this socket. When
     * a socket is connected to a remote address, packets may only be sent to
     * or received from that address. By default a datagram socket is not
     * connected.
     */
    public void connect(Address address, int port) {
        if (!isConnected) {

            remoteAddress = address;
            remotePort = port;

            isConnected = true;
            isClosed = false;

            //Logger.getLogger("log").logln(USR.ERROR, "AppSocket: connect to " + remoteAddress + ":" +  remotePort);
        } else {
            throw new Error("Cannot connect while already connected");
        }
    }

    /**
     * Connects the socket to a remote address for this socket. When
     * a socket is connected to a remote address, packets may only be sent to
     * or received from that address. By default a datagram socket is not
     * connected.
     */
    public void connect(SocketAddress sockaddr) {
        connect(sockaddr.getAddress(), sockaddr.getPort());
    }

    /**
     * Returns the remote port for this socket.
     * Returns -1 if the socket is not connected.
     */
    public int getPort() {
        if (isConnected) {
            return remotePort;
        } else {
            return -1;
        }
    }

    /**
     * Returns the address to which this socket is connected.
     * Returns null if the socket is not connected.
     */
    public Address getRemoteAddress() {
        if (isConnected) {
            return remoteAddress;
        } else {
            return null;
        }
    }

    /**
     * Returns the connection state of the socket.
     * @return true if the socket succesfuly connected to a server
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Returns the bound state of the socket.
     * @return true if the socket succesfuly bound to an address
     */
    public boolean isBound() {
        return isBound;
    }

    /**
     * Returns the bound state of the socket.
     * @return true if the socket is closed.
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Returns the local port for this socket
     * to which this socket is bound.
     */
    public int getLocalPort() {
        if (isBound) {
            return localPort;
        } else {
            return -1;
        }
    }

    /**
     * Returns the address of the endpoint this socket is bound to,
     * or null if it is not bound yet. */
    public Address getLocalAddress() {
        return localAddress;
    }

    /**
     * Send a datagram from this socket.
     * The Datagram includes information indicating the data to be sent,
     * its length, the address of the remote router, and the port number
     * on the remote router.
     */
    public void send(Datagram dg) throws SocketException, NoRouteToHostException {
        if (isClosed) {
            throw new SocketException("Socket closed");
        }

        if (isBound) {
            dg.setSrcPort(localPort);
        }

        if (isConnected) {
            dg.setDstAddress(remoteAddress);
            dg.setDstPort(remotePort);
        }

        if (appSockMux.sendDatagram(dg) == false) {
            Logger.getLogger("log").logln(USR.ERROR, "AppSocket: forwardDatagram queue full in ASM");
        }
    }

    /**
     * Receives a datagram from this socket. When this method returns, the
     * Datagram is filled with the data received. The datagram also contains
     * the sender's  address, and the port number on the sender's machine.
     * This method blocks until a datagram is received.
     *
     * TODO: check if this needs to be synchronized for multiple threads
     */
    public Datagram receive() throws SocketException {
        if (isClosed) {
            throw new SocketException("Socket closed");
        }

        takeThread = Thread.currentThread();
        try {
            return queue.take();
        } catch (InterruptedException ie) {
            if (isClosed) {
                Logger.getLogger("log").logln(USR.STDOUT, "AppSocket: port " + localPort + " closed on shutdown");
                return null;
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "AppSocket: port " + localPort + " receive interrupted");
                throw new ClosedByInterruptException(Integer.toString(localPort));
            }
        }
    }

    /**
     * Disconnects the socket.
     * This does nothing if the socket is not connected.
     */
    public void disconnect() {
        if (isConnected) {

            remoteAddress = null;
            remotePort = 0;

            isConnected = false;

            //Logger.getLogger("log").logln(USR.ERROR, "AppSocket: disconnect");
        }
    }

    /**
     * Close this socket.
     */
    public void close() {
        if (!isClosed) {

            appSockMux.removeAppSocket(this);

            isClosed = true;

            if (takeThread != null) {
                takeThread.interrupt();
            }

        }
    }

    /**
     * toString.
     */
    @Override
	public String toString() {
        return "Socket[" + (isBound ? "bound " : "") + (isConnected ? "connected " : "") + "addr=" + localAddress + " port=" +
               remotePort + " localport=" + localPort + "]";

    }

}
