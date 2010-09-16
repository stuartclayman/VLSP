package usr.router;

/**
 * A Listener of NetIFs.
 */
public interface NetIFListener {
    /**
     * A NetIF has a datagram.
     */
    public boolean datagramArrived(NetIF netIF);

    /**
     * A NetIF is closing.
     */
    public boolean netIFClosing(NetIF netIF);
}
