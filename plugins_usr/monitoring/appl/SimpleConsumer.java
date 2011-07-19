// SimpleConsumer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.appl;

import usr.applications.*;
import usr.net.SocketAddress;
import usr.net.GIDAddress;
import plugins_usr.monitoring.distribution.USRDataPlaneConsumerWithNames;
import eu.reservoir.monitoring.appl.BasicConsumer;
import java.util.Scanner;
import java.util.Properties;

/**
 * This receives measurements from a USR Data Plane.
 * It is run as an Application.
 */
public class SimpleConsumer implements Application {
    // The Basic consumer
    BasicConsumer consumer;

    // The port to listen on
    int dataPort;

    /**
     * Construct a SimpleConsumer using USR
     */
    public SimpleConsumer() {
    }

    /**
     * Init.
     */
    public ApplicationResponse init(String[] args) {
        if (args.length == 1) {
            Scanner sc = new Scanner(args[0]);
            dataPort = sc.nextInt();
            return new ApplicationResponse(true, "");
        } else {
            return new ApplicationResponse(false, "usage: SimpleConsumer port");
        }
    }

    /**
     * Start.
     */
    public ApplicationResponse start() {
        try {
            // set up a BasicConsumer
            consumer = new BasicConsumer();

            // set up multicast address for data
            SocketAddress address = new SocketAddress(dataPort);

            // set up data plane
            consumer.setDataPlane(new USRDataPlaneConsumerWithNames(address));

            consumer.connect();

            return new ApplicationResponse(true, "");
        } catch (Exception e) {
            return new ApplicationResponse(false, e.getMessage());
        }
    }

    /**
     * Stop.
     */
    public ApplicationResponse stop() {
        consumer.disconnect();

        synchronized (this) {
            notifyAll();
        }

        return new ApplicationResponse(true, "");
    }

    public void run() {
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException ie) {
        }
    }
}
