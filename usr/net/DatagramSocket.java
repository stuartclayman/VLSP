package usr.net;

import usr.router.AppSocket;
import usr.router.Router;
import usr.router.RouterDirectory;
import usr.net.SocketTimeoutException;
import java.net.SocketException;
import java.net.NoRouteToHostException;

/**
 * A DatagramSocket looks similar to a Java DatagramSocket.
 * It is intended to be a replacement for the Java version
 * in order that apps can use USR for networking without <em>too much</em>
 * effort in rewrites.
 */
public class DatagramSocket {
    AppSocket socketImpl;

    /**
     * Create a DatagramSocket bound to a a free port.
     */
    public DatagramSocket() throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        AppSocket appSocket = new AppSocket(router);

        socketImpl = appSocket;
    }

    /**
     * Create a DatagramSocket bound to a port.
     */
    public DatagramSocket(int port) throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        AppSocket appSocket = new AppSocket(router, port);

        socketImpl = appSocket;
    }

    /**
     * Create a DatagramSocket connected to a specified remote Address and port.
     */
    public DatagramSocket(Address addr, int port) throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        AppSocket appSocket = new AppSocket(router, addr, port);

        socketImpl = appSocket;
    }

    /**
     * Create a DatagramSocket connected to a specified remote Address and port.
     */
    public DatagramSocket(int port, Address addr) throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        AppSocket appSocket = new AppSocket(router, addr, port);

        socketImpl = appSocket;
    }

    /**
     * Create a DatagramSocket connected to a specified Socket Address.
     */
    public DatagramSocket(SocketAddress socketAddr) throws SocketException {
        this(socketAddr.getAddress(), socketAddr.getPort());
    }

    /**
     * Binds this DatagramSocket to a port.
     */
    public void bind(int port) throws SocketException {
        socketImpl.bind(port);
    }

    /**
     * Connects the socket to a remote address for this socket. When
     * a socket is connected to a remote address, packets may only be sent to
     * or received from that address. By default a datagram socket is not
     * connected.
     */
    public void connect(SocketAddress sockaddr) {
        socketImpl.connect(sockaddr);
    }

    /**
     * Connects the socket to a remote address for this socket. When
     * a socket is connected to a remote address, packets may only be sent to
     * or received from that address. By default a datagram socket is not
     * connected.
     */
    public void connect(Address address, int port) {
        socketImpl.connect(address, port);
    }

    /**
     * Returns the remote port for this socket.
     * Returns -1 if the socket is not connected.
     */
    public int getPort() {
        return socketImpl.getPort();
    }

    /**
     * Returns the address to which this socket is connected.
     * Returns null if the socket is not connected.
     */
    public Address getRemoteAddress() {
        return socketImpl.getRemoteAddress();
    }

    /**
     * Returns the connection state of the socket.
     * @return true if the socket succesfuly connected to a server
     */
    public boolean isConnected() {
        return socketImpl.isConnected();
    }

    /**
     * Returns the bound state of the socket.
     * @return true if the socket succesfuly bound to an address
     */
    public boolean isBound() {
        return socketImpl.isBound();
    }

    /**
     * Returns the bound state of the socket.
     * @return true if the socket is closed.
     */
    public boolean isClosed() {
        return socketImpl.isClosed();
    }

    /**
     * Returns the local port for this socket
     * to which this socket is bound.
     */
    public int getLocalPort() {
        return socketImpl.getLocalPort();
    }

    /**
     * Returns the address of the endpoint this socket is bound to,
     * or null if it is not bound yet. */
    public Address getLocalAddress() {
        return socketImpl.getLocalAddress();
    }

    /**
     * Send a datagram from this socket.
     * The Datagram includes information indicating the data to be sent,
     * its length, the address of the remote router, and the port number
     * on the remote router.
     */
    public void send(Datagram dg) throws SocketException, NoRouteToHostException {
        socketImpl.send(dg);
    }

    /**
     * Receives a datagram from this socket. When this method returns, the
     * Datagram is filled with the data received. The datagram also contains
     * the sender's  address, and the port number on the sender's machine.
     * This method blocks until a datagram is received.
     */
    public Datagram receive() throws SocketException, SocketTimeoutException {
        return socketImpl.receive();
    }

    /**
     * Close this socket.
     */
    public void close() {
        socketImpl.close();
    }

    /**
     * Disconnects the socket.
     * This does nothing if the socket is not connected.
     */
    public void disconnect() {
        socketImpl.disconnect();
    }

    /**
     * Retrive setting for SO_TIMEOUT. 0 returns implies that the option is disabled
     * (i.e., timeout of infinity).
     */
    public int getSoTimeout() throws SocketException {
        return socketImpl.getSoTimeout();
    }

    /**
     * Enable/disable SO_TIMEOUT with the specified timeout, in
     * milliseconds. With this option set to a non-zero timeout, a call to
     * receive() for this DatagramSocket will block for only this amount of
     * time. If the timeout expires, a java.net.SocketTimeoutException is
     * raised, though the DatagramSocket is still valid. The option must be
     * enabled prior to entering the blocking operation to have effect. The
     * timeout must be &gt; 0. A timeout of zero is interpreted as an infinite
     * timeout.
     * Parameters: timeout - the specified timeout in milliseconds.
     */
    public void setSoTimeout(int timeout) throws SocketException {
        socketImpl.setSoTimeout(timeout);
    }

    /**
     * toString.
     */
    public String toString() {
        return socketImpl.toString();
    }

}
