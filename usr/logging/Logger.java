// Logger.java

package usr.logging;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * An object that will be a logger.
 * It keeps a set of output objects which actually do the logging,
 * and for each one there is a bitmask which determines which
 * log message will be accepted by which output.
 * <p>
 * This allows us to set up a set of outputs and then configure them
 * at run-time to accept certain log statements.
 * It also allows the same log element to go to multiple log outputs
 * if they have the same bit set in their BitMask.
 */
public class Logger implements Logging {
    /*
     * The name of the logger
     */
    String name;

    /*
     * A map of output object to its BitMask.
     */
    Map<Object, BitMask> outputs = null;

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
        outputs = new HashMap<Object, BitMask>();
    }

    /**
     * Log a message using a Strng.
     */
    @Override
	public void log(BitMask mask, String msg) {
        doLog(mask, msg, false);
    }

    /**
     * Log a message using a Strng.
     * Add a trailing newline.
     */
    @Override
	public void logln(BitMask mask, String msg) {
        doLog(mask, msg, true);
    }

    /**
     * Log using a LogInput object.
     */
    @Override
	public void log(BitMask mask, LogInput obj) {
        doLog(mask, obj, false);
    }

    /**
     * Log a message using a Strng.
     */
    public void log(int mask, String msg) {
        doLog(new BitMask(mask), msg, false);
    }

    /**
     * Log a message using a Strng.
     * Add a trailing newline.
     */
    public void logln(int mask, String msg) {
        doLog(new BitMask(mask), msg, true);
    }

    /**
     * Log using a LogInput object.
     */
    public void log(int mask, LogInput obj) {
        doLog(new BitMask(mask), obj, false);
    }

    /**
     * Add output to a printwriter
     */
    @Override
	public Logger addOutput(PrintWriter w) {
        addOutputLog(w, new BitMask());
        return this;
    }

    /**
     * Add output to a printstream.
     */
    @Override
	public Logger addOutput(PrintStream s) {
        addOutputLog(s, new BitMask());
        return this;
    }

    /**
     * Add output to a ByteChannel.
     */
    @Override
	public Logger addOutput(ByteChannel ch) {
        addOutputLog(ch, new BitMask());
        return this;
    }

    /**
     * Add output to an LogOutput object.
     */
    @Override
	public Logger addOutput(LogOutput eo) {
        addOutputLog(eo, new BitMask());
        return this;
    }

    /**
     * Add output to a printwriter
     */
    public Logger addOutput(PrintWriter w, BitMask mask) {
        addOutputLog(w, mask);
        return this;
    }

    /**
     * Add output to a printstream.
     */
    public Logger addOutput(PrintStream s, BitMask mask) {
        addOutputLog(s, mask);
        return this;
    }

    /**
     * Add output to a ByteChannel.
     */
    public Logger addOutput(ByteChannel ch, BitMask mask) {
        addOutputLog(ch, mask);
        return this;
    }

    /**
     * Add output to an LogOutput object.
     */
    public Logger addOutput(LogOutput eo, BitMask mask) {
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
    public Logger removeOutput(PrintStream s) {
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
    public BitMask getMaskForOutput(Object output) {
        if (outputs == null) {
            return null;
        } else {
            return outputs.get(output);
        }
    }

    /**
     * Set a mask for a an output object.
     */
    public Logger setMaskForOutput(Object output, BitMask mask) {
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
    private void addOutputLog(Object output, BitMask mask) {
        if (outputs == null) {
            outputs = new HashMap<Object, BitMask>();
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
    private void doLog(BitMask mask, Object message, boolean trailingNL) {
        if (outputs == null) {
            return;
        }

        Iterator<Object> outputI = outputs.keySet().iterator();

        while (outputI.hasNext()) {
            Object anOutput = outputI.next();

            // get mask for output
            BitMask aMask = outputs.get(anOutput);

            // check if we need to send to it
            BitMask acceptMask = aMask.and(mask);

            // if the result has more than 0 bits set then
            // the current output will accept the current message
            if (!(acceptMask.isClear())) {  // empty
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
            ((PrintWriter)anOutput).flush();
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
            ((PrintStream)anOutput).flush();
        } else if (anOutput instanceof ByteChannel) {
            try {
                if (message instanceof LogInput) {
                    String msg = ((LogInput)message).logView();
                    ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                    ((ByteChannel)anOutput).write(buffer);
                } else {
                    if (trailingNL) {
                        String msg = ((String)message) + "\n";
                        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                        ((ByteChannel)anOutput).write(buffer);
                    } else {
                        String msg = (String)message;
                        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                        ((ByteChannel)anOutput).write(buffer);
                    }
                }
            } catch (IOException ioe) {
            }
        } else {
            ;
        }
    }

}