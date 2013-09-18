package usr.router;

import usr.logging.Logger;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class VectorRouterFabric extends AbstractRouterFabric implements RouterFabric, NetIFListener, DatagramDevice {
    /**
     * Construct a VectorRouterFabric.
     */
    public VectorRouterFabric(Router r, RouterOptions opt) {
        super(r, opt);
    }

    /**
     * Create a new empty routing table
     */
    @Override
	public RoutingTable newRoutingTable() {
        return new VectorRoutingTable(4);
    }

    /**
     * Create a new routing table from a transmitted byte[]
     */
    @Override
	public RoutingTable decodeRoutingTable(byte[] bytes, NetIF netif) throws Exception {
        RoutingTable table = new VectorRoutingTable(bytes, netif);

        Logger.getLogger("log").logln(1<<6,
                                      leadin() + "\nsize " + bytes.length + " = 6+" +
                                      (bytes.length-6) + " -> " + table.showTransmitted());

        return table;

    }

}