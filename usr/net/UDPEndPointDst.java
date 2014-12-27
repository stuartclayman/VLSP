package usr.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import usr.logging.Logger;
import usr.logging.USR;
import usr.common.ANSI;

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
    @Override
    public boolean connect() throws IOException {
        //Logger.getLogger("log").logln(USR.STDOUT, ANSI.YELLOW + "UDPEndPointDst " + " connect " + ANSI.RESET_COLOUR);

        if (isConnected) {
            throw new IOException("Cannot connect again to: " + socket);
        } else {            
            isConnected = true;
            return true;
        }
    }

    /**
     * Set the remote InetAddress and port
     */
    public void setRemoteAddress(InetAddress addr, int port) throws IOException {
        //Logger.getLogger("log").logln(USR.STDOUT, ANSI.YELLOW + "UDPEndPointDst " + " setRemoteAddress " + addr + ":" + port + ANSI.RESET_COLOUR);


        if (socket.isConnected()) { 
            Logger.getLogger("log").logln(USR.STDOUT, ANSI.RED + "UDPEndPointDst " + " already connected" + ANSI.RESET_COLOUR);
        } else {
            socket.connect(addr, port);

            System.err.println("UDPEndPointDst connect " +  socket.getLocalAddress() + ":" + socket.getLocalPort() + " to " + socket.getInetAddress() + ":" + socket.getPort());


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
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * TO String
     */
    @Override
    public String toString() {
        if (socket == null) {
            return "0.0.0.0" + ":" + port + " (no socket)";
        } else {
            if (! socket.isClosed()) {
                return socket.getLocalAddress().getHostName() + ":" + port + (socket.isConnected() ? " (connected)" : " (NOT connected)");
            } else {
                return  "0.0.0.0" + ":" + port + " (CLOSED)";
            }
        }
    }

}
