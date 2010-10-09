package usr.router;
import usr.net.Datagram;

import usr.logging.*;
/**
 * A Listener of NetIFs.
 */
public interface NetIFListener {
    /**
     * A NetIF has a datagram.
     */
    public boolean datagramArrived(NetIF netIF, Datagram datagram);

    /**
     * A NetIF is closing.
     */
    public boolean netIFClosing(NetIF netIF);
}
