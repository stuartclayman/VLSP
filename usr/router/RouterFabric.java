package usr.router;

import java.util.List;

import usr.net.Address;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public interface RouterFabric {
    /**
     * Get it's name
     */
    public String getName();

    /**
     * Add a Network Interface to this Router.
     */
    public RouterPort addNetIF(NetIF netIF);

    /**
     * Remove a Network Interface from this Router.
     */
    public boolean removeNetIF(NetIF netIF);

    /**
     * Get the local NetIF that has the sockets.
     */
    public NetIF getLocalNetIF();

    /**
     * Get port N.
     */
    public RouterPort getPort(int p);

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    public List<RouterPort> listPorts();

    /**
     * Get a list of all connected Network Interfaces
     */
    public List<NetIF> listNetIF();

    /** Return the interface which connects to a given host */
    public NetIF findNetIF(String host);

    /**
     * Set the netIF weight associated with a link to a certain router name
     */
    public boolean setNetIFWeight(String name, int weight);

    /**
     * Close ports
     */
    public void closePorts();

    /**
     * Send goodbye message to all ports
     */
    public void sendGoodbye();

    /**
     * Close port.
     */
    public void closePort(RouterPort port);

    /**
     * initialisation
     */
    public boolean init();

    /**
     * Start me up.
     */
    public boolean start();

    /**
     * Stop the RouterController.
     */
    public boolean stop();

    /**
     * Get the Router this Fabric is part of
     */
    public Router getRouter();

    /**
     * Create a new empty routing table
     */
    public RoutingTable newRoutingTable();

    /**
     * Create a new routing table from a transmitted byte[]
     */
    public RoutingTable decodeRoutingTable(byte[] bytes, NetIF netif) throws Exception;


    /** List Routing table */

    public RoutingTable getRoutingTable();

    /** Ping a given id -- expect a response */
    public boolean ping(Address addr);

    /** Echo -- send datagram to id */
    public boolean echo(Address addr);


    /**
     * Get the state 
     */
    public FabricState getState();

    /**
     * The states of the RouterFabric
     */
    public enum FabricState {
        PRE_INIT,        // initial state
        POST_INIT,       // after for init()
        STARTED,         // we have entered started
        STOPPING,        // we have called stop() and the RouterFabric should stop
        STOPPED          // the RouterFabric is stopped
    }



}
