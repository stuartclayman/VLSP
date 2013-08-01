package usr.net;

import java.io.IOException;
import usr.logging.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

/**
 * A source end point for connections over TCP.
 */
public class TCPEndPointSrc implements TCPEndPoint {
    // Host
    String host;

    // Port
    int port;

    // socket
    Socket socket;
    // and channel
    SocketChannel channel;

    // is connected
    boolean isConnected;

    /**
     * A TCPEndPointSrc needs a host and port for the TCPEndPointDst.
     */
    public TCPEndPointSrc(String host, int port) throws UnknownHostException, IOException {
        this.host = host;
        this.port = port;
        isConnected = false;

        channel = SocketChannel.open();
    }

    /**
     * Connect
     */
    public boolean connect() throws IOException {
        if (isConnected) {
            throw new IOException("Cannot connect again to: " + socket);
        } else {
            socket = channel.socket();
            //socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(host, port));
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
    public Socket getSocket() {
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
