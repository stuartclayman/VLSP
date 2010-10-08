// Logger.java

package usr.logging;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.PrintStream;


/**
 * An object that will be a logger.
 * It keeps a set of output objects which actually do the logging,
 * and for each one there is a bitset/mask which determines which
 * log message will be accepted by which output.
 * <p>
 * This allows us to set up a set of outputs and then configure them
 * at run-time to accept certain log statements.
 * It also allows the same log element to go to multiple log outputs
 * if they have the same bit set in their BitSet.
 */
public class Logger implements Logging {
    /*
     * The name of the logger
     */
    String name;

    /*
     * A map of output object to its BitSet.
     */
    Map <Object,BitSet>outputs = null;

    /*
     * A static map of Logger objects
     */
    static Map<String, Logger> loggerMap = new HashMap<String, Logger>();

    /**
     * A static method that returns a Logger by name.
     * If the Logger with that name already exists it is returned,
     * otherwise it is created first.
     */
    public static Logger getLogger(String name) {
        Logger logger = loggerMap.get(name);

        if (logger == null) {
            // we don't know this Logger
            // so create it
            logger = new Logger(name);
            // and put it in the map
            loggerMap.put(name, logger);
        }

        return logger;        
    }

    /**
     * Construct a Logger object.
     */
    public Logger(String name) {
        this.name = name;
        outputs = new HashMap<Object,BitSet>();
    }

    /**
     * Log a message using a Strng.
     */
    public void log(BitSet mask, String msg) {
	doLog(mask, msg, false);
    }

    /**
     * Log a message using a Strng.
     * Add a trailing newline.
     */
    public void logln(BitSet mask, String msg) {
	doLog(mask, msg, true);
    }

    /**
     * Log using a LogInput object.
     */
    public void log(BitSet mask, LogInput obj) {
        doLog(mask, obj, false);
    }

    /**
     * Add output to a printwriter
     */
    public Logger addOutput(PrintWriter w) {
	addOutputLog(w, new BitSet());
	return this;
    }

    /**
     * Add output to a printstream.
     */
    public Logger addOutput(PrintStream s){
	addOutputLog(s, new BitSet());
	return this;
    }


    /**
     * Add output to an LogOutput object.
     */
    public Logger addOutput(LogOutput eo) {
	addOutputLog(eo, new BitSet());
	return this;
    }

    /**
     * Add output to a printwriter
     */
    public Logger addOutput(PrintWriter w, BitSet mask) {
	addOutputLog(w, mask);
	return this;
    }

    /**
     * Add output to a printstream.
     */
    public Logger addOutput(PrintStream s, BitSet mask){
	addOutputLog(s, mask);
	return this;
    }


    /**
     * Add output to an LogOutput object.
     */
    public Logger addOutput(LogOutput eo, BitSet mask) {
	addOutputLog(eo, mask);
	return this;
    }

    /**
     * Remove output to a printwriter
     */
    public Logger removeOutput(PrintWriter w) {
	removeOutputLog(w);
        return this;
    }

    /**
     * Remove output to a printstream.
     */
    public Logger removeOutput(PrintStream s){
	removeOutputLog(s);
        return this;
    }


    /**
     * Remove output to an LogOutput object.
     */
    public Logger removeOutput(LogOutput eo) {
	removeOutputLog(eo);
        return this;
    }

    /**
     * Get a mask for a an output object.
     */
    public BitSet getMaskForOutput(Object output) {
	if (outputs == null) {
	    return null;
	} else {
	    return (BitSet)outputs.get(output);
	}
    }

    /**
     * Set a mask for a an output object.
     */
    public Logger setMaskForOutput(Object output, BitSet mask) {
	if (outputs == null) {
	    return this;
	} else {
	    outputs.put(output, mask);
            return this;
	}
    }

	

    /**
     * Add an output to the outputs map.
     */
    private void addOutputLog(Object output, BitSet mask) {
	if (outputs == null) {
	    outputs = new HashMap<Object,BitSet>();
	}

	outputs.put(output, mask);
    }

    /**
     * Remove an output to the outputs map.
     */
    private void removeOutputLog(Object output) {
	if (outputs == null) {
	    return;
	} else {
	    outputs.remove(output);
	}
    }

    /**
     * Visit each output object and determine if it will
     * accept a log message, and if so log it.
     */
    private void doLog(BitSet mask, Object message, boolean trailingNL) {
	if (outputs == null) {
	    return;
        }

	Iterator outputI = outputs.keySet().iterator();

	while (outputI.hasNext()) {
	    Object anOutput = outputI.next();

	    // we need to clone becasue ops on BitSets are in-place
	    // and destructive :-(
	    BitSet aMask = (BitSet)outputs.get(anOutput);
	    BitSet acceptMask = (BitSet)aMask.clone();

	    acceptMask.and(mask);

	    // if the result has more than 0 bits set then
	    // the current output will accept the current message
	    if (! (acceptMask.cardinality() == 0)) { // empty
		dispatch(message, anOutput, trailingNL);
           }
        }
    }

    /**
     * Dispatch a message to an output object.
     * @param message the message, either a String or a LogInput
     * @param anOutput the output object, 
     */
    private void dispatch(Object message, Object anOutput, boolean trailingNL) {
	// if output is a LogOutput pass on the message
        // as it knows how to deal with it
	if (anOutput instanceof LogOutput) {
	    if (message instanceof LogInput) {
		((LogOutput)anOutput).process((LogInput)message);
	    } else {
                if (trailingNL) {
                    String msg = (String)message;
                    ((LogOutput)anOutput).process(msg + "\n");
                } else {
                    ((LogOutput)anOutput).process(((String)message));
                }
	    }

	} else if (anOutput instanceof PrintWriter) {
	    if (message instanceof LogInput) {
		((PrintWriter)anOutput).print(((LogInput)message).logView());
	    } else {
                if (trailingNL) {
                    ((PrintWriter)anOutput).println((String)message);
                } else {
                    ((PrintWriter)anOutput).print((String)message);
                }
	    }

	} else if (anOutput instanceof PrintStream) {
	    if (message instanceof LogInput) {
		((PrintStream)anOutput).print(((LogInput)message).logView());
	    } else {
                if (trailingNL) {
                    ((PrintStream)anOutput).println((String)message);
                } else {
                    ((PrintStream)anOutput).print((String)message);
                }
	    }

	} else {
	    ;
	}
    }
		    
}
