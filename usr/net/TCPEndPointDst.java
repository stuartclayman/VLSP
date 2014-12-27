package usr.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A source end point for connections over TCP.
 */
public class TCPEndPointDst implements TCPEndPoint {
    // Port
    int port;

    // listen socket
    ServerSocket serverSocket;
    // socket for connection
    Socket socket;

    // is connected
    boolean isConnected;

    /**
     * A TCPEndPointDst needs a port to listen on.
     */
    public TCPEndPointDst(ServerSocket serverSocket) throws UnknownHostException, IOException {
        this.serverSocket = serverSocket;
        this.port = serverSocket.getLocalPort();
        isConnected = false;
    }

    /**
     * Connect
     */
    @Override
    public boolean connect() throws IOException {
        if (isConnected) {
            throw new IOException("Cannot connect again to: " + socket);
        } else {
            socket = serverSocket.accept();
            isConnected = true;
            return true;
        }
    }

    /**
     * Get the remote host.
     */
    public InetAddress getRemoteHost() {
        return socket.getInetAddress();
    }

    /**
     * Get the remote host.
     */
    public int getRemotePort() {
        return socket.getPort();
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
    public Socket getSocket() {
        return socket;
    }

    /**
     * TO String
     */
    @Override
    public String toString() {
        if (socket == null) {
            return serverSocket.getInetAddress().getHostName() + ":" + port + " (no socket)";
        } else {
            return serverSocket.getInetAddress().getHostName() + ":" + port + " (connected)";
        }
    }

}
