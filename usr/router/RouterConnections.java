package usr.router;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;


public interface RouterConnections {
    /**
     * Get the port for the connection port
     */
    public int getConnectionPort();

    /**
     * Start the connections listening.
     */
    public boolean start();


    /**
     * Stop the listener.
     */
    public boolean stop();


    /**
     * Return a hash code for a NetIF in Create Connection.
     */
    public int getCreateConnectionHashCode(NetIF netIF, InetAddress addr, int port);

    /**
     * Create an EndPoint source for a NetIF
     */
    public NetIF getNetIFSrc(String host, int connectionPort) throws UnknownHostException, IOException;


}
