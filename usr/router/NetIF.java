package usr.router;

import usr.net.Address;
import usr.net.Datagram;
import java.util.Map;
import java.net.Socket;

/**
 * A Network Interface for a Router.
 */
public interface NetIF {
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
     * Send a Datagram.
     */
    public boolean sendDatagram(Datagram dg);

    /**
     * Read a Datagram.
     */
    public Datagram readDatagram();
}
