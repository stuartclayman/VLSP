// Logging.java

package usr.logging;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.channels.ByteChannel;

/**
 * An interface for objects that want to be a logger.
 */
public interface Logging {
    /**
     * Log a message using a Strng.
     */
    public void log(BitMask mask, String msg);

    /**
     * Log a message using a Strng.
     * Add a trailing newline.
     */
    public void logln(BitMask mask, String msg);

    /**
     * Log using a LogInput object.
     */
    public void log(BitMask mask, LogInput obj);

    /**
     * Add output to a printwriter
     */
    public Logger addOutput(PrintWriter w);

    /**
     * Add output to a printstream.
     */
    public Logger addOutput(PrintStream s);

    /**
     * Add output to a ByteChannel.
     */
    public Logger addOutput(ByteChannel ch);

    /**
     * Add output to an LogOutput object.
     */
    public Logger addOutput(LogOutput eo);
}