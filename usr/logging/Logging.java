// Logging.java

package usr.logging;

import java.io.PrintWriter;
import java.io.PrintStream;
import java.util.BitSet;

/**
 * An interface for objects that want to be a logger.
 */
public interface Logging {
    /**
     * Log a message using a Strng.
     */
    public void log(BitSet mask, String msg);

    /**
     * Log using a LogInput object.
     */
    public void log(BitSet mask, LogInput obj);

    /**
     * Add output to a printwriter
     */
    public Logger addOutput(PrintWriter w);

    /**
     * Add output to a printstream.
     */
    public Logger addOutput(PrintStream s);

    /**
     * Add output to an LogOutput object.
     */
    public Logger addOutput(LogOutput eo);
}
