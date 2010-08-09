// MCRPException.java


package usr.interactor;

/**
 * An MCRPException is thrown if there is a problem interacting
 * with the router.
 */
public class MCRPException extends Exception {
    /**
     * Construct a MCRPException
     */
    public MCRPException() {
	super();
    }

    /**
     * Construct a MCRPException with a message
     */
    public MCRPException(String s) {
	super(s);
    }

    /**
     * Construct a MCRPException with a throwable
     */
    public MCRPException(Throwable t) {
	super(t);
    }
}
