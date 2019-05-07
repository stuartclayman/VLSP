// JvmListener.java


package usr.localcontroller;


import usr.common.ProcessListener;

public class JvmListener implements ProcessListener {
    protected LocalController localController;
    
    /**
     * A JvmListener for a LocalController
     */
    public JvmListener(LocalController localController) {
        this.localController = localController;
    }


    /**
     * The Process has gone away and the input stream was closed.
     */
    public synchronized void processEnded(Process p, String name) {
        //System.err.println("JvmListener: processEnded Process: " + p + " " + name);
    }

    /**
     * The Process has gone away and this is the exitValue
     */
    public void processExitValue(Process p, int exitValue, String name) {
        //System.err.println("JvmListener: processExitValue Process: " + p + " " + name + " " + exitValue);
    }


}
