// LogInput.java

package usr.logging;

/**
 * An interface for objects that will act as input for a logger.
 */
public interface LogInput {
    /**
     * Get a view of this logger input as a string
     */
    public String logView();
}