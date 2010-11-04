package usr.router;
import usr.net.Datagram;

import usr.logging.*;
/**
 * A Listener of NetIFs.
 */
public interface NetIFListener {
    /**
     * Pass the NetIFListener a new Datagram from a NetIF.
     */
    public boolean datagramArrived(NetIF netIF, Datagram datagram);

    /**
     * Can the NetIFListener accept a new datagram.
     */
    public boolean canAcceptDatagram(NetIF netIF);

    /**
     * Can the NetIFListener route this datagram.
     */
    public boolean canRoute(Datagram dg);

    /**
     * A NetIF is closing.
     */
    public boolean netIFClosing(NetIF netIF);
}
