// LoggingOutputStream.java

package usr.logging;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A LoggingOutputStream acts as a gateway between OutputStreams
 * and the Logging system.
 * We can set an OutputStream object which puts all data to Logging.
 */
public class LoggingOutputStream extends OutputStream {
    /**
     * A buffer for the message to be collected.
     */
    ByteArrayOutputStream buffer = null;

    /**
     * The logging object the this OutputStream interacts with.
     */
    Logger theLogger = null;

    /**
     * The mask for messages from this OutputStream.
     */
    BitMask mask = null;

    /**
     * Create a LoggingOutputStream using a specified logger.
     */
    public LoggingOutputStream(Logger logger) {
        this(logger, new BitMask());
    }

    /**
     * Create a LoggingOutputStream using a specified logger
     * and BitMask mask.
     */
    public LoggingOutputStream(Logger logger, BitMask m) {
        buffer = new ByteArrayOutputStream();
        theLogger = logger;
        mask = m;
    }

    /**
     * The write method needed for an OutputStream.
     */
    @Override
	public void write(int b) throws IOException {
        buffer.write(b);
    }

    /**
     * Flush this OutputStream.
     */
    @Override
	public void flush() throws IOException {
        doLogging(buffer.toString());
        buffer.reset();
    }

    /**
     * This is where the logger is called.
     * The default is to log using a String, but subclasses
     * can decide to log using a LogInput object.
     */
    public void doLogging(String s) {
        if (theLogger == null) {
            return;
        } else {
            theLogger.log(mask, s);
        }
    }

    /**
     * Set the mask for this LoggingOutputStream.
     */
    public void setMask(BitMask m) {
        mask = m;
    }

    /**
     * Get the mask for this LoggingOutputStream.
     */
    public BitMask getMask() {
        return mask;
    }

    /**
     * Set the logger for this LoggingOutputStream.
     */
    public void setLogger(Logger l) {
        theLogger = l;
    }

    /**
     * Get the logger for this LoggingOutputStream.
     */
    public Logger getLogger() {
        return theLogger;
    }

}