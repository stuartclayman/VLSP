package demo_usr.paths;

import usr.net.*;
import usr.logging.*;
import usr.applications.*;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.Scanner;
import java.nio.ByteBuffer;
import us.monoid.json.*;

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
    public ApplicationResponse init(String[] args) {
        return super.init(args);
    }

    /**
     * Start application
     */
    public ApplicationResponse start() {
        return super.start();
    }

    /**
     * Implement graceful shut down 
     */
    public ApplicationResponse stop() {
        return super.stop();
    }


    /** 
     * Run the application 
     */
    public void run() {
        super.run();
    }



}
