package usr.router;

import java.util.List;
import usr.logging.*;
import java.util.ArrayList;
import usr.net.*;
import java.net.*;
import usr.protocol.Protocol;
import java.nio.ByteBuffer;
import java.lang.*;
import java.util.*;
import java.net.NoRouteToHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    public RoutingTable newRoutingTable() {
        return new SimpleRoutingTable();
    }

    /**
     * Create a new routing table from a transmitted byte[]
     */
    public RoutingTable decodeRoutingTable(byte[] bytes, NetIF netif) throws Exception {
        RoutingTable table = new SimpleRoutingTable(bytes, netif);

        Logger.getLogger("log").logln(1<<6,
                                      leadin() + "\nsize " + bytes.length + " = 5+" +
                                      (bytes.length-5) + " -> " + table.showTransmitted());

        return table;
    }

}