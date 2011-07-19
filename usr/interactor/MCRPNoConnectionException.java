// MCRPNoConnectionException.java

package usr.interactor;

/**
 * An MCRPNoConnectionException is thrown if the
 * MCRPInteractor is not ready to accept another request.
 */
public class MCRPNoConnectionException extends MCRPException {
    /**
     * Construct a MCRPNoConnectionException
     */
    public MCRPNoConnectionException() {
        super();
    }

    /**
     * Construct a MCRPNoConnectionException with a message
     */
    public MCRPNoConnectionException(String s) {
        super(s);
    }

    /**
     * Construct a MCRPNoConnectionException with a throwable
     */
    public MCRPNoConnectionException(Throwable t) {
        super(t);
    }
}
