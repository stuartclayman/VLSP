// MCRPNoConnectionException.java


package usr.globalcontroller;

/**
 * An XMLNoTagException is thrown if parsing XML where
 a tag is optional and it is not present
 */
public class XMLNoTagException extends Exception {
    /**
     * Construct a XMLNoTagException
     */
    public XMLNoTagException() {
	super();
    }

    /**
     * Construct a XMLNoTagException
     */
    public XMLNoTagException(String s) {
	super(s);
    }


}
