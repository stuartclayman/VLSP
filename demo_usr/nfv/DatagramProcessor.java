package demo_usr.nfv;

import usr.net.Datagram;

public interface DatagramProcessor {

    /**
     * Receive a Datagram
     */
    public Datagram receiveDatagram();

    /**
     * Process the recevied Datagram
     */
    public boolean processDatagram(Datagram dg);

    /**
     * Forward the recevied Datagram, possibly.
     * If processDatagram() returns false this will not be called.
     */
    public boolean forwardDatagram(Datagram dg);

}
