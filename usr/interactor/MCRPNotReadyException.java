// MCRPNotReadyException.java

package usr.interactor;
import usr.logging.*;

/**
 * An MCRPNotReadyException is thrown if the
 * MCRPInteractor is not ready to accept another request.
 */
public class MCRPNotReadyException extends MCRPException {
    /**
     * Construct a MCRPNotReadyException
     */
    public MCRPNotReadyException() {
	super();
    }

    /**
     * Construct a MCRPNotReadyException with a message
     */
    public MCRPNotReadyException(String s) {
	super(s);
    }

    /**
     * Construct a MCRPNotReadyException with a throwable
     */
    public MCRPNotReadyException(Throwable t) {
	super(t);
    }
}
