// LoggingOutputStream.java

package usr.logging;

import java.util.BitSet;
import java.io.*;

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
    BitSet mask = null;

    /**
     * Create a LoggingOutputStream using a specified logger.
     */
    public LoggingOutputStream(Logger logger) {
	this(logger, new BitSet());
    }
	
    /**
     * Create a LoggingOutputStream using a specified logger
     * and BitSet mask.
     */
    public LoggingOutputStream(Logger logger, BitSet m) {
        buffer = new ByteArrayOutputStream();
        theLogger = logger;
	mask = m;
    }
	
    /**
     * The write method needed for an OutputStream.
     */
    public void write(int b) throws IOException {
	buffer.write(b);
    }

    /**
     * Flush this OutputStream.
     */
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
    public void setMask(BitSet m) {
	mask = m;
    }

    /**
     * Get the mask for this LoggingOutputStream.
     */
    public BitSet getMask() {
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
