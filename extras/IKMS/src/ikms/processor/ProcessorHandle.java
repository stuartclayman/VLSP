package ikms.processor;


/**
 * A handle on an Processor.
 * It holds the name, the Processor itself, and its state.
 */
public class ProcessorHandle implements Runnable {
    // The name
    String name;

    // The thread name
    String threadName;

    // The proc
    Processor proc;

    // The args
    String[] args;

    // The proc ID
    int procID;

    // Start Time
    long startTime;

    // The state
    ProcessorState state;

    // The ProcessorManager
    ProcessorManager manager;

    /**
     * Construct an ProcessorHandle
     */
    ProcessorHandle(ProcessorManager procMgr, String name, Processor proc, String[] args, int procID) {
        this.name = name;
        this.proc = proc;
        this.args = args;
        this.procID = procID;
        this.manager = procMgr;
        this.startTime = System.currentTimeMillis();
        setState(ProcessorState.PROC_POST_INIT);
    }

    /**
     * Get the Processor name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Processor
     */
    public Processor getProcessor() {
        return proc;
    }

    /**
     * Get the args for the Proc.
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * Get the Proc ID.
     */
    public int getID() {
        return procID;
    }


    /**
     * Get the start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the thread name
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Set the thread name
     */
    ProcessorHandle setThreadName(String name) {
        threadName = name;
        return this;
    }

    /**
     * Get the state
     */
    public ProcessorState getState() {
        return state;
    }

    /**
     * Set the state
     */
    ProcessorHandle setState(ProcessorState s) {
        state = s;
        return this;
    }

    /**
     * This run() delegates to Processor run()
     */
    public void run() {
        System.err.println( "ProcessorHandle: entering run: " + proc);

        if (getState() == ProcessorHandle.ProcessorState.RUNNING) {
            proc.run();
        }

        System.err.println( "ProcessorHandle: exiting run: " + proc + " with state of: " + getState());

        // if we get to the end of run() and the proc
        // is still in the RUNNING state,
        // we need to stop it
        if (getState() == ProcessorHandle.ProcessorState.RUNNING) {
            setState(ProcessorHandle.ProcessorState.PROC_POST_RUN);
            manager.stopProcessor(getName());
        }

    }

    /**
     * The states of the proc
     */
    public enum ProcessorState {
        PROC_POST_INIT,   // after for init()
        RUNNING,         // we have entered run()
        PROC_POST_RUN,    // the proc dropped out of run(), without a stop()
        STOPPING,          // we have called stop() and the the proc should stop
        STOPPED          // the proc is stopped
    }



}
