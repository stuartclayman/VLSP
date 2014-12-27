package usr.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.UnknownHostException;

/**
 * A source end point for connections over UDP.
 */
public class UDPEndPointSrc implements UDPEndPoint {
    // Host
    String host;

    // Port
    int port;

    // InetAddress of host
    InetAddress inetAddr;

    // socket
    DatagramSocket socket;

    // isConnected
    boolean isConnected;

    /**
     * A UDPEndPointSrc needs a host and port for the UDPEndPointDst.
     */
    public UDPEndPointSrc(String host, int port) throws UnknownHostException, IOException {
        this.host = host;
        this.port = port;
        isConnected = false;

        inetAddr = InetAddress.getByName(host);
    }

    /**
     * Connect
     */
    @Override
    public boolean connect() throws IOException {
        if (isConnected) {
            throw new IOException("Cannot connect again to: " + socket);
        } else {
            socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));

            if (host.equals("localhost")) {
                socket.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port));
            } else {
                socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
            }

            System.err.println("UDPEndPointSrc connect " +  socket.getLocalAddress() + ":" + socket.getLocalPort() + " to " + socket.getInetAddress() + ":" + socket.getPort());
            isConnected = true;
            return true;
        }
    }

    /**
     * Get the remote host.
     */
    public InetAddress getRemoteHost() {
        return inetAddr;
    }

    /**
     * Get the port no.
     */
    public int getRemotePort() {
        return port;
    }

    /**
     * Get the port no.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the Socket.
     */
    @Override
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * TO String
     */
    @Override
    public String toString() {
        if (socket == null) {
            return host + ":" + port + " (no socket)";
        } else {
            return host + ":" + port + (socket.isConnected() ? " (connected)" : " (NOT connected)");
        }
    }

}
