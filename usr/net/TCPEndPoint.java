package usr.net;

import java.net.Socket;

/**
 * An End Point of a Connection built over TCP.
 */
public interface TCPEndPoint extends EndPoint {

    /**
     * Get the Socket.
     */
    public Socket getSocket();


}