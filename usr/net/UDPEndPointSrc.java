package usr.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;

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
    // and channel
    DatagramChannel channel;

    // isConnected
    boolean isConnected;

    /**
     * A UDPEndPointSrc needs a host and port for the UDPEndPointDst.
     */
    public UDPEndPointSrc(String host, int port) throws UnknownHostException, IOException {
        this.host = host;
        this.port = port;
        isConnected = false;

        channel = DatagramChannel.open();
    }

    /**
     * Connect
     */
    @Override
	public boolean connect() throws IOException {
        if (isConnected) {
            throw new IOException("Cannot connect again to: " + socket);
        } else {
            socket = channel.socket();
            //socket = new DatagramSocket();
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
            return host + ":" + port + " ? ";
        } else {
            return host + ":" + port + " -> ";
        }
    }

}
