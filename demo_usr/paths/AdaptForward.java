package demo_usr.paths;

import usr.applications.Application;
import usr.applications.ApplicationResponse;

/**
 * An application for adapting a Forward node
 * to send to a new USR port.
 * It sends a message to the Forward node listening
 * on a Management port on usrAddr:usrPort 
 * and informs it to update the forwarding
 * USR address  usr-addr:usr-port.
 * <p>
 * AdaptForward usrAddr:usrPort usr-addr:usr-port [optionals]
 * Optional args:
 * -d start-up delay (in milliseconds)
 * -v verbose
 */
public class AdaptForward extends AdaptIngress implements Application {
    /**
     * Constructor for AdaptForward
     */
    public AdaptForward() {
    }

    /**
     * Initialisation for AdaptForward
     *  usrAddr:usrPort usr-addr:usr-port [optionals]
     * Optional args:
     * -d start-up delay (in milliseconds)
     * -v verbose
     */
    @Override
	public ApplicationResponse init(String[] args) {
        return super.init(args);
    }

    /**
     * Start application
     */
    @Override
	public ApplicationResponse start() {
        return super.start();
    }

    /**
     * Implement graceful shut down 
     */
    @Override
	public ApplicationResponse stop() {
        return super.stop();
    }


    /** 
     * Run the application 
     */
    @Override
	public void run() {
        super.run();
    }



}
