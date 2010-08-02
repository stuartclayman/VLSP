package usr.router;

import java.util.List;
import java.util.ArrayList;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric implements RouterFabric {
    // The Router this is fabric for
    Router router;

    // A List of RouterPorts
    ArrayList<RouterPort> ports;

    /**
     * Construct a SimpleRouterFabric.
     */
    public SimpleRouterFabric(Router router) {
        this.router = router;

        int limit = 32;
        ports = new ArrayList<RouterPort>(limit);
        for (int p=0; p < limit; p++) {
            resetPort(p);
        }
    }

    /**
     * Add a Network Interface to this Router.
     */
    public RouterPort addNetIF(NetIF netIF) {
        int nextFree = findNextFreePort();

        RouterPort rp = new RouterPort(nextFree, netIF);

        ports.set(nextFree, rp);

        System.err.println("RF: plugged NetIF: " + netIF + " into port " + nextFree);

        return rp;
    }

    /**
     * Remove a Network Interface from this Router.
     */
    public boolean removeNetIF(NetIF netIF) {
        // find port associated with netIF
        RouterPort port = findNetIF(netIF);

        if (port != null) {
            // disconnect netIF from port
            resetPort(port.getPortNo());
            return true;
        } else {
            // didn't find netIF in any RouterPort
            return false;
        }
    }

    /**
     * Get port N.
     */
    public RouterPort getPort(int p) {
        return ports.get(p);
    }

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    public List<RouterPort> listPorts() {
        return ports;
    }

    /*
     * Port processing
     */

    /**
     * Reset a port
     */
    void resetPort(int p) {
        ports.add(p, RouterPort.EMPTY);
    }

    /**
     * Find the port a NetIF is in.
     * Skip through all ports to find a NetIF
     * @return null if a NetIF is not found.
     */
    RouterPort findNetIF(NetIF netIF) {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            RouterPort port = ports.get(p);
            if (port.getNetIF().equals(netIF)) {
                return port;
            }
        }

        return null;
    }
    
    /**
     * Find the next free port to use.
     * Start at port 0 and work way up.
     */
    int findNextFreePort() {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            if (ports.get(p).equals(RouterPort.EMPTY)) {
                return p;
            }
        }

        // if we get here the ports are all full
        // so make more
        ports.ensureCapacity(limit + 8);
        for (int p = limit; p < (limit + 8); p++) {
            resetPort(p);
        }

        return limit;
    }
}
