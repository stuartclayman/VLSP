package usr.net;

import java.io.IOException;
import usr.logging.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * A source end point for connections over UDP.
 */
public class UDPEndPointSrc implements UDPEndPoint {
    // Host
    String host;

    // Port
    int port;

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
    }

    /**
     * Connect
     */
    public boolean connect() throws IOException {
        if (isConnected) {
            throw new IOException("Cannot connect again to: " + socket);
        } else {
            socket = new DatagramSocket();
            isConnected = true;
            return true;
        }
    }

    /**
     * Get the remote host.
     */
    public String getHostName() {
        return host;
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
            return host + ":" + port + " ? ";
        } else {
            return host + ":" + port + " -> ";
        }
    }

}