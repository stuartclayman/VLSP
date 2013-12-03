// SimpleConsumer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.net.SocketAddress;
import usr.net.GIDAddress;
import plugins_usr.monitoring.distribution.USRDataPlaneConsumerWithNames;
import eu.reservoir.monitoring.appl.BasicConsumer;
import java.util.Scanner;

/**
 * This receives measurements from a USR Data Plane.
 */
public class SimpleConsumer {
    // The Basic consumer
    BasicConsumer consumer;

    /*
     * Construct a SimpleConsumer using USR
     */
    public SimpleConsumer(int dataPort) {
        // set up a BasicConsumer
        consumer = new BasicConsumer();

        // set up multicast address for data
        SocketAddress address = new SocketAddress(dataPort);

        // set up data plane
        consumer.setDataPlane(new USRDataPlaneConsumerWithNames(address));

        consumer.connect();

    }

    public static void main(String [] args) {
        int appPort = 2299;

        if (args.length == 0) {
        } else if (args.length == 1) {
            Scanner sc = new Scanner(args[0]);
            appPort = sc.nextInt();
            sc.close();
        } else {
            System.err.println("usage: SimpleConsumer port");
            System.exit(1);
        }

        // Set up Router
        try {
            int port = 18191;
            int r2r = 18192;

            RouterEnv routerEnv = new RouterEnv(port, r2r, "Router-2");
            Router router = routerEnv.getRouter();

            // check
            if (router.isActive()) {
            } else {
                throw new Exception("Router failed to start");
            }

            // set ID
            router.setAddress(new GIDAddress(2));


        } catch (Exception e) {
            System.err.println("SimpleConsumer exception: " + e);
            e.printStackTrace();
            System.exit(2);
        }

        // Set up Consumer on AppSocket 2299
        new SimpleConsumer(appPort);

        System.err.println("SimpleConsumer listening on " + "localnet" + "/" + appPort);

    }

}