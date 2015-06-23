package demo_usr.viewer;

import java.io.IOException;
import java.lang.reflect.Field;

import usr.common.ProcessWrapper;

/**
 * A process that runs down the end of a pipe.
 */
class PipeProcess extends ProcessWrapper {
    StringBuilder builder;
    String pipeData;
    boolean error = false;

    /**
     * A ProcessWrapper wraps a Process with a name.
     */
    public PipeProcess(Process proc){
        super(proc, "pipe-" + proc.hashCode());
        builder = new StringBuilder();
    }

    /**
     * Print out some input.
     */
    @Override
    public void print(String label, String line){
        // could check if label is 'stderr' or 'stdout'
        // and do different things
        if (label.equals("stderr")) {
            System.err.println("PipeProcess: stderr " + line);
        } else {
            // it's stdout
            //System.err.println("PipeProcess: stdout " + line);
            builder.append(line);
            builder.append("\n");
        }
    }

    /**
     * It's EOF
     * Converts builder to Strng
     * Or set null, if error
     */
    @Override
    public void eof(){
        System.err.println("PipeProcess: EOF");
        if (error)
            pipeData = null;
        else
            pipeData = builder.toString();
    }

    /**
     * There has been an IO error
     */
    @Override
    public void ioerror(String label, IOException ioe){
        System.err.println("PipeProcess: " + label + " Got IOException " + ioe);
        error = true;
        pipeData = null;
        //stop();
    }

    /**
     * Get process ID
     */
    public int getPID(){
        try {
            Process process = getProcess();
            Field field = process.getClass().getDeclaredField("pid");
            field.setAccessible(true);
            return field.getInt(process);
        }   catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get the data
     * Returns null if an error has occured.
     */
    public String getData(){
        if (error) {
            return null;
        } else {
            if (pipeData == null)
                return null;
            else
                return pipeData;
        }
    }

    /**
     * Stop the process wrapper.
     */
    @Override
    public void stop(){
        try {
            super.stop();

            iThread.join();
            eThread.join();
        } catch (Exception e) {
        }
    }

    @Override
    protected void destroy(){
    }
}
