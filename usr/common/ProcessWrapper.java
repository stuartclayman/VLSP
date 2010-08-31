// ProcessWrapper.java

package usr.common;

import java.io.*;

/**
 * This class wraps a Process
 * and collects up the output and error streams.
 */
public class ProcessWrapper {
    // The Process
    Process process;

    // The name
    String name;

    // InputStream thread
    Thread iThread;

    // ErrorStream thread
    Thread eThread;

    /**
     * A ProcessWrapper wraps a Process with a name.
     */
    public ProcessWrapper(Process proc, String name) {
        process = proc;
        this.name = name;

        // allocate a ProcessListener for the InputStream
        ProcessListener iListener = new ProcessListener(proc.getInputStream(), "stdout", this);
        // allocate a ProcessListener for the ErrorStream
        ProcessListener eListener = new ProcessListener(proc.getErrorStream(), "stderr", this);

        // allocate a Thread for the InputStream Listener
        iThread = new Thread(iListener);
        // allocate a Thread for the ErrorStream Listener
        eThread = new Thread(eListener);

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
        System.out.println(label + " " + getName() + " " + line);
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
         * Main Loop.
         */
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
                    running = false;
                }
            }

            //wrapper.print(label, "EOF");

        }
    }
}
