package usr.router;

import usr.net.Address;
import usr.net.Datagram;
import java.util.Map;
import java.net.Socket;
import java.io.IOException;

/**
 * A Network Interface for a Router.
 */
public interface NetIF {
    /**
     * Connect
     */
    public boolean connect() throws IOException;

    /**
     * Get the name of this NetIF.
     */
    public String getName();


    /**
     * Set the name of this NetIF.
     */
    public NetIF setName(String name);

    /**
     * Get the ID of this NetIF.
     */
    public int getID();

    /**
     * Set the ID of this NetIF.
     */
    public NetIF setID(int id);

    /**
     * Get the weight of this NetIF.
     */
    public int getWeight();

    /**
     * Set the weight of this NetIF.
     */
    public NetIF setWeight(int w);

    /**
     * Get the Address for this connection.
     */
    public Address getAddress();

    /**
     * Set the Address for this connection.
     */
    public NetIF setAddress(Address addr);

    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    public String getRemoteRouterName();


    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    public NetIF setRemoteRouterName(String name);

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    public Address getRemoteRouterAddress();

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    public NetIF setRemoteRouterAddress(Address addr);

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
    public Map<String, Number> getStats();

    /**
     * Send a Datagram originating at this host (sets src address)
     */
    public boolean sendDatagram(Datagram dg);

    /**
     * forward a datagram (does not set src address)
     */
    public boolean forwardDatagram(Datagram dg);


    /**
     * Read a Datagram.
     */
    //public Datagram readDatagram();


    /**
     * Close a NetIF
     */
    public void close();

    /**
     * Is closed.
     */
    public boolean isClosed();

    /**
     * Get the Listener of a NetIF.
     */
    public NetIFListener getNetIFListener();

    /**
     * Set the Listener of NetIF.
     */
    public NetIF setNetIFListener(NetIFListener l);
    
    /** Remote close received */
    public void remoteClose();
    
    /** Routing table sent */
    public boolean sendRoutingTable(String s);
    
    
}
