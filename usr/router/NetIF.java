package usr.router;

import java.io.IOException;
import java.net.InetAddress;

import usr.net.Address;

/**
 * A Network Interface for a Router.
 */
public interface NetIF extends DatagramDevice {
    /**
     * Connect - phase 1
     */
    public boolean connectPhase1() throws IOException;

    /**
     * Connect - phase 2
     */
    public boolean connectPhase2() throws IOException;

    /**
     * Get the ID of this NetIF.
     */
    public int getID();

    /**
     * Set the ID of this NetIF.
     */
    public void setID(int id);

    /**
     * Get the weight of this NetIF.
     */
    public int getWeight();

    /**
     * Set the weight of this NetIF.
     */
    public void setWeight(int w);

    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    public String getRemoteRouterName();


    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterName(String name);

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    public Address getRemoteRouterAddress();

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterAddress(Address addr);

    /**
     * Get the interface stats.
     * A map of values like:
     * "in_bytes" -> in_bytes
     * "in_packets" -> in_packets
     * "in_errors" -> in_errors
     * "in_dropped" -> in_dropped
     * "out_bytes" -> out_bytes
     * "out_packets" -> out_packets
     * "out_errors" -> out_errors
     * "out_dropped" -> out_dropped
     */
    public NetStats getStats();

    /** Close a NetIF
     */
    public void close();

    /** Is this a local interface */
    public boolean isLocal();

    /**
     * Is closed.
     */
    public boolean isClosed();


    /** Remote close received */
    public void remoteClose();

    /**
     * Get the RouterPort a NetIF is plugIged into.
     */
    public RouterPort getRouterPort();

    /**
     * Set the RouterPort a NetIF is plugIged into.
     */
    public void setRouterPort(RouterPort rp);

    /**
     * Get the remote address to which this socket is connected.
     */
    public InetAddress getInetAddress();

    /**
     * Get the remote port number to which this socket is connected.
     */
    public int getPort();

    /**
     * Gets the local address to which the socket is bound.
     */
    public InetAddress getLocalAddress();

    /**
     * Get the port number on the local host to which this socket is bound.
     */
    public int getLocalPort();

    /**
     * Set the remote InetAddress and port
     */
    public void setRemoteAddress(InetAddress addr, int port) throws IOException;

}
