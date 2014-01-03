package ikms.processor;

import ikms.core.Response;

/**
 * A Processor is a Runnable object that
 * has a managed lifecycle.
 */
public interface Processor extends Runnable {
    /**
     * Initialize with some args
     */
    public Response init(String[] args);

    /**
     * Start an application.
     * This is called before run().
     */
    public Response start();


    /**
     * Stop an application.
     * This is called to implement graceful shut down
     * and cause run() to end.
     */
    public Response stop();

    /**
     * Close down a Processor
     */
    public Response close();
    

}
