package usr.router;

import usr.logging.Logger;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric extends AbstractRouterFabric implements RouterFabric, NetIFListener, DatagramDevice {
    /**
     * Construct a SimpleRouterFabric.
     */
    public SimpleRouterFabric(Router r, RouterOptions opt) {
        super(r, opt);
    }

    /**
     * Create a new empty routing table
     */
    @Override
	public RoutingTable newRoutingTable() {
        return new SimpleRoutingTable();
    }

    /**
     * Create a new routing table from a transmitted byte[]
     */
    @Override
	public RoutingTable decodeRoutingTable(byte[] bytes, NetIF netif) throws Exception {
        RoutingTable table = new SimpleRoutingTable(bytes, netif);

        Logger.getLogger("log").logln(1<<6,
                                      leadin() + "\nsize " + bytes.length + " = 5+" +
                                      (bytes.length-5) + " -> " + table.showTransmitted());

        return table;
    }

}