// ProcessWrapper.java

package usr.common;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import usr.common.TimedThreadGroup;
import usr.common.TimedThread;

import usr.logging.Logger;
import usr.logging.USR;

/**
 * This class wraps a Process
 * and collects up the output and error streams.
 */
public class ProcessWrapper {
    // The Process
    protected Process process;

    // The name
    String name;

    // InputStream thread
    protected ProcessStreamHandler iStreamHandler;
    protected Thread iThread;

    // ErrorStream thread
    protected ProcessStreamHandler eStreamHandler;
    protected Thread eThread;

    // Is the prcoess stopped
    protected boolean isStopped = true;
    
    // Is the prcoess EOF
    protected boolean isEof = true;


    // The process exit value
    protected int exitValue = -1;

    // ProcessListener
    protected ProcessListener listener = null;

    // Stream Ident
    public enum StreamIdent { Stdout, Stderr };
    
    /**
     * A ProcessWrapper wraps a Process with a name.
     */
    public ProcessWrapper(Process proc, String name) {
        this(proc, name,  "stdout",  "stderr", null);
    }
    
    /**
     * A ProcessWrapper wraps a Process with a name.
     */
    public ProcessWrapper(Process proc, String name, ProcessListener listener) {
        this(proc, name,  "stdout",  "stderr", listener);
    }
    
    /**
     * A ProcessWrapper wraps a Process with a name, plus labels for stdout and stderr
     */
    public ProcessWrapper(Process proc, String name, String stdoutLabel, String stderrLabel) {
        this(proc, name, stdoutLabel, stderrLabel, null);
    }

    /**
     * A ProcessWrapper wraps a Process with a name, plus labels for stdout and stderr
     */
    public ProcessWrapper(Process proc, String name, String stdoutLabel, String stderrLabel, ProcessListener listener) {
        process = proc;
        this.name = name;

        // it's running
        isStopped = false;
        isEof = false;

        // ProcessListener
        this.listener = listener;

        // allocate a ProcessStreamHandler for the OutputStream of the process
        // which is the InputStream from the Process
        iStreamHandler = new ProcessStreamHandler(proc.getInputStream(), StreamIdent.Stdout, stdoutLabel, this);
        // allocate a ProcessStreamHandler for the ErrorStream
        eStreamHandler = new ProcessStreamHandler(proc.getErrorStream(), StreamIdent.Stderr, stderrLabel, this);

        // allocate a Thread for the InputStream Listener
        iThread = new TimedThread(iStreamHandler, name + "-InputStream");
        // allocate a Thread for the ErrorStream Listener
        eThread = new TimedThread(eStreamHandler, name + "-ErrorStream");

        // start both threads
        iThread.start();
        eThread.start();

    }

    /**
     * Get the Process.
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Get the name
     */
    public String getName() {
        return name;
    }

    /**
     * Is it stopped
     */
    public boolean isStopped() {
        return isStopped;
    }

    /**
     * Is it EOF, probably because the process has gone away
     */
    public boolean isEof() {
        return isEof;
    }

    /**
     * Terminated successfullt
     */
    public boolean terminatedOK() {
        return (exitValue == 0);
    }

    /**
     * Print out some input.
     */
    public void print(StreamIdent ident, String label, String line) {
        // could check if StreamIdent is 'Stderr' or 'Stdout'
        // and do different things
        if (ident == StreamIdent.Stderr) {
            Logger.getLogger("log").logln(USR.ERROR, label + " " + getName() + " " + line);
        } else {
            Logger.getLogger("log").logln(USR.STDOUT, label + " " + getName() + " " + line);
        }
    }

    /**
     * It's EOF
     * By default nothing special happens.
     */
    public void eof(StreamIdent ident) {
        //System.err.println("ProcessWrapper: EOF " + ident);
        isEof = true;

        if (listener != null) {
            listener.processEnded(process, name);
        }

        stop();
    }

    /**
     * There has been an IO error
     */
    public void ioerror(StreamIdent ident, IOException ioe) {
        System.err.println("ProcessWrapper: " + ident + " Got IOException " + ioe);
        stop();
    }

    /**
     * Stop the process wrapper.
     */
    public void stop() {
        synchronized (this) {
            if (!isStopped()) {
                try {
                    isStopped = true;

                    // disconnect the process
                    //System.err.println("ProcessWrapper: STOPPING "+name);

                    //System.err.println("ProcessWrapper: close input");
                    // The Process OutputStream is the stdin of the real process
                    // Closed it.  Like Ctrl D.
                    process.getOutputStream().close();

                    // wait a bit to see if it shuts on it's own
                    try {
                        Thread.sleep(100);
                    } catch (java.lang.InterruptedException e) {
                    }

                    // Close the process stdout
                    process.getInputStream().close();

                    /*
                     * close of input streams moved into thread for better reliability
                     */

                    // stop listeners
                    iStreamHandler.stop();
                    eStreamHandler.stop();

                    // now splat it
                    terminate();
            
                    //iThread.join();
                    //eThread.join();
                //} catch (InterruptedException ie) {
                } catch (IOException ioe) {
                    ioe.getMessage();
                }
            }
        }
    }

    protected void terminate() {
        try {
            exitValue = process.exitValue();

            if (listener != null) {
                listener.processExitValue(process, exitValue, name);
            }
            //System.out.println("ProcessWrapper: Process " + process + " terminate");
            
        } catch (IllegalThreadStateException e) {
            //the subprocess represented by this Process object has not yet terminated
            process.destroy();
        }
    }

    /**
     * Listen on an InputStream
     */
    class ProcessStreamHandler implements Runnable {
        // The InputStream
        InputStream input;

        // Label to this Listener
        String label;

        // The ProcessWrapper
        ProcessWrapper wrapper;

        // StreamIdent
        StreamIdent ident;

        // running ?
        boolean running = false;

        /**
         * Construct a ProcessStreamHandler
         */
        public ProcessStreamHandler(InputStream is, StreamIdent ident, String label, ProcessWrapper wrapper) {
            input = is;
            this.ident = ident;
            this.label = label;
            this.wrapper = wrapper;
        }

        /**
         * End the Listener.
         */
        public synchronized void stop() {
            notifyAll();
            running = false;
        }

        /**
         * Main Loop.
         */
        @Override
        public void run() {
            running = true;

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;

            while (running) {
                try {
                    if ((line = reader.readLine()) != null) {
                        // callback to the wrapper to get printout
                        wrapper.print(ident, label, line);
                    } else {
                        // EOF
                        running = false;
                    }
                } catch (IOException ieo) {
                    // error
                    wrapper.ioerror(ident, ieo);
                    running = false;
                }
            }


            try {
                //System.err.println("ProcessStreamHandler: close " + label);
                input.close();
            } catch (IOException ioe) {
		wrapper.ioerror(ident, ioe);
            }

            //wrapper.print(label, "EOF");
            wrapper.eof(ident);

        }

    }
}
