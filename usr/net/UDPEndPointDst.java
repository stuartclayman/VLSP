package usr.net;

import java.io.IOException;
import usr.logging.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * A source end point for connections over UDP.
 */
public class UDPEndPointDst implements UDPEndPoint {
    // Port
    int port;

    // listen socket
    DatagramSocket socket;

    // isConnected
    boolean isConnected;

    /**
     * A UDPEndPointDst needs a port to listen on.
     */
    public UDPEndPointDst(DatagramSocket socket) throws UnknownHostException, IOException {
        this.socket = socket;
        this.port = socket.getLocalPort();
        isConnected = false;
    }

    /**
     * Connect
     */
    public boolean connect() throws IOException {
        if (isConnected) {
            throw new IOException("Cannot connect again to: " + socket);
        } else {
            isConnected = true;
            return true;
        }
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
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * TO String
     */
    public String toString() {
        if (socket == null) {
            return " @ " + socket.getInetAddress().getHostName() + ":" + port;
        } else {
            return " -> " + socket.getInetAddress().getHostName() + ":" + port;
        }
    }

}