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
    protected ProcessListener iListener;
    protected Thread iThread;

    // ErrorStream thread
    protected ProcessListener eListener;
    protected Thread eThread;

    /**
     * A ProcessWrapper wraps a Process with a name.
     */
    public ProcessWrapper(Process proc, String name) {
        process = proc;
        this.name = name;

        // allocate a ProcessListener for the InputStream
        iListener = new ProcessListener(proc.getInputStream(), "stdout", this);
        // allocate a ProcessListener for the ErrorStream
        eListener = new ProcessListener(proc.getErrorStream(), "stderr", this);

        // allocate a Thread for the InputStream Listener
        iThread = new TimedThread(iListener, name + "-InputStream");
        // allocate a Thread for the ErrorStream Listener
        eThread = new TimedThread(eListener, name + "-ErrorStream");

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
     * Print out some input.
     */
    public void print(String label, String line) {
        // could check if label is 'stderr' or 'stdout'
        // and do different things
        if (label.equals("stderr")) {
            Logger.getLogger("log").logln(USR.ERROR, label + " " + getName() + " " + line);
        } else {
            Logger.getLogger("log").logln(USR.STDOUT, label + " " + getName() + " " + line);
        }
    }

    /**
     * It's EOF
     * By default nothing special happens.
     */
    public void eof() {
    }

    /**
     * There has been an IO error
     */
    public void ioerror(String label, IOException ioe) {
        //System.out.println("ProcessWrapper: " + label + " Got IOException " + ioe);
        stop();
    }

    /**
     * Stop the process wrapper.
     */
    public void stop() {
        try {
            // disconnect the process
            //System.err.println("ProcessWrapper: STOPPING "+name);

            //System.err.println("ProcessWrapper: close input");
            process.getOutputStream().close();

            // wait a bit to see if it shuts on it's own
            try {
                Thread.sleep(1000);
            } catch (java.lang.InterruptedException e) {
            }

            process.getInputStream().close();

            /*
             * close of input streams moved into thread for better reliability
             */

            // stop listeners
            iListener.stop();
            eListener.stop();

            // now splat it
            destroy();

        } catch (IOException ioe) {
            ioe.getMessage();
        }
    }

    protected void destroy() {
        try {
            int exitValue = process.exitValue();
        } catch (IllegalThreadStateException e) {
            //the subprocess represented by this Process object has not yet terminated
            process.destroy();
        }
    }

    /**
     * Listen on an InputStream
     */
    class ProcessListener implements Runnable {
        // The InputStream
        InputStream input;

        // Label to this Listener
        String label;

        // The ProcessWrapper
        ProcessWrapper wrapper;

        // running ?
        boolean running = false;

        /**
         * Construct a ProcessListener
         */
        public ProcessListener(InputStream is, String label, ProcessWrapper wrapper) {
            input = is;
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
                        wrapper.print(label, line);
                    } else {
                        // EOF
                        running = false;
                    }
                } catch (IOException ieo) {
                    // error
                    wrapper.ioerror(label, ieo);
                    running = false;
                }
            }


            try {
                //System.err.println("ProcessListener: close " + label);
                input.close();
            } catch (IOException ioe) {
		wrapper.ioerror(label, ioe);
            }

            //wrapper.print(label, "EOF");
            wrapper.eof();

        }

    }
}
