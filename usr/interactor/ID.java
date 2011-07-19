// ID.java

package usr.interactor;
import usr.logging.*;

/**
 * An ID returned in a response.
 * Some responses send IDs.
 */
public class ID {
    String theID = null;

    /**
     * Construct an ID from a String.
     */
    public ID(String id) {
        theID = id;
    }

    /**
     * Get the underlying value
     */
    public String getValue() {
        return theID;
    }

    /**
     * To string
     */
    public String toString() {
        return theID;
    }

    /**
     * equals.
     */
    public boolean equals(Object obj) {
        if (obj instanceof ID) {
            if (this.theID.equals(((ID)obj).theID)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
