package usr.router;

import java.net.*;
import usr.net.Address;
import usr.logging.*;
import usr.net.Datagram;
import java.util.Map;
import java.net.Socket;
import java.io.IOException;

/**
 * A Network Interface for a Router.
 */
public interface NetIF extends DatagramDevice {
    /**
     * Connect
     */
    public boolean connect() throws IOException;


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

}