package usr.applications;

import java.net.SocketException;
import java.util.Scanner;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.DatagramSocket;

/**
 * An application for Receiving some data
 */
public class Echo implements Application {
    String[] strings = {};

    boolean running = false;

    /**
     * Constructor for Echo
     */
    public Echo() {
    }

    /**
     * Initialisation for Echo.
     * Echo port
     */
    @Override
    public ApplicationResponse init(String[] args) {
        if (args.length != 0) {
            // try arg
            strings  = args;

            return new ApplicationResponse(true, "");

        } else {
            return new ApplicationResponse(false, "Usage: Echo args");
        }
    }

    /** Start application with argument  */
    @Override
    public ApplicationResponse start() {
        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    @Override
    public ApplicationResponse stop() {
        running = false;

        Logger.getLogger("log").logln(USR.STDOUT, "Echo stop");

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    @Override
    public void run() {
        StringBuilder builder = new StringBuilder();
        for (String str : strings) {
            builder.append(str);
            builder.append(" ");
        }

        Logger.getLogger("log").log(USR.STDOUT, builder.toString());
    }

}
