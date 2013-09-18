package usr.net;

import java.net.DatagramSocket;

/**
 * An End Point of a Connection built over UDP.
 */
public interface UDPEndPoint extends EndPoint {

    /**
     * Get the DatagramSocket.
     */
    public DatagramSocket getSocket();


}