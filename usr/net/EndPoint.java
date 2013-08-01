package usr.net;

import java.io.IOException;
import usr.logging.*;

/**
 * An End Point of a Connection.
 */
public interface EndPoint {

    /**
     * Connect
     */
    public boolean connect() throws IOException;

}