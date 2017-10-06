package usr.router;

import usr.net.Datagram;

/**
 * An interface for classes that can capture datagrams from a FabricDevice.
 * Datagrams are copied onto the DatagramCapture class.
 */
public interface DatagramCapture {
    /**
     * The DatagramCapture object is sent a datagram.
     */
    public boolean sendDatagram(Datagram dg);
}
