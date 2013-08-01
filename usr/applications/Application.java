package usr.applications;

/**
 * An Application has is a Runnable object that
 * has a managed lifecycle.
 */
public interface Application extends Runnable {
    /**
     * Initialize with some args
     */
    public ApplicationResponse init(String[] args);

    /**
     * Start an application.
     * This is called before run().
     */
    public ApplicationResponse start();


    /**
     * Stop an application.
     * This is called to implement graceful shut down
     * and cause run() to end.
     */
    public ApplicationResponse stop();
}