package usr.net;

import java.io.IOException;

/**
 * An End Point of a Connection.
 */
public interface EndPoint {

    /**
     * Connect
     */
    public boolean connect() throws IOException;

    /**
     * Get the port no.
     */
    public int getPort();

}
