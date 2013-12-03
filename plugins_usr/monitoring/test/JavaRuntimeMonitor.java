// JavaRuntimeMonitor.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.test;

import eu.reservoir.demo.InfraProbe;
import usr.router.Router;
import usr.router.RouterEnv;
import usr.net.*;
import usr.interactor.RouterInteractor;
import plugins_usr.monitoring.distribution.USRDataPlaneProducerWithNames;
import eu.reservoir.monitoring.appl.BasicDataSource;
import eu.reservoir.monitoring.core.DataSource;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import java.util.Scanner;

/**
 * This monitor sends java runtime data  uses a USR Data Plane.
 */
public class JavaRuntimeMonitor {
    // The DataSource
    DataSource ds;

    /*
     * Construct a JavaRuntimeMonitor
     */
    public JavaRuntimeMonitor(Address addr, int dataPort) {
        // set up data source
        ds = new BasicDataSource();

        // set up socket address for data
        SocketAddress address = new SocketAddress(addr, dataPort);

        // set up data plane
        ds.setDataPlane(new USRDataPlaneProducerWithNames(address));

        ds.connect();
    }

    private void turnOnProbe(Probe p) {
        ds.addProbe(p);
        ds.turnOnProbe(p);
    }

    @SuppressWarnings("unused")
    private void turnOffProbe(Probe p) {
        ds.deactivateProbe(p);
        ds.removeProbe(p);
    }

    public static void main(String [] args) {
        Address addr = new GIDAddress(2);
        int appPort = 2299;

        // the host that has the Router at addr
        String remHost = "localhost";
        int remPort = 18191;

        if (args.length == 0) {
            // use existing settings
        } else if (args.length == 2) {
            Scanner sc = new Scanner(args[0]);
            addr = new GIDAddress(sc.nextInt());
            sc.close();
            sc = new Scanner(args[1]);
            appPort = sc.nextInt();
            sc.close();

        } else {
            System.err.println("JavaRuntimeMonitor GID-address port");
            System.exit(1);
        }

        // Set up Router
        // And connect to Router @(2)
        try {
            int port = 18181;
            int r2r = 18182;

            RouterEnv routerEnv = new RouterEnv(port, r2r, "Router-1");
            Router router = routerEnv.getRouter();

            // check
            if (router.isActive()) {
            } else {
                throw new Exception("Router failed to start");
            }

            // set up id
            router.setAddress(new GIDAddress(1));

            // connnect to the other router
            // first we tal kto my own ManagementConsole
            RouterInteractor selfInteractor = new RouterInteractor("localhost", 18181);

            // then set up Router-to-Router data connection
            selfInteractor.createConnection(remHost + ":" + remPort, 20);

            // and stop talking to the ManagementConsole
            selfInteractor.quit();


        } catch (Exception e) {
            System.err.println("Cannot interact with router at " + remHost + ":" + remPort);
            System.exit(1);
        }

        // we got a hostname
        JavaRuntimeMonitor hostMon = new JavaRuntimeMonitor(addr, appPort);

        //Probe javaProbe = new JavaMemoryProbe("localhost" + ".javaMemory");
        Probe javaProbe = new InfraProbe("uk.ac.ucl.ee.Service1", "localhost");
        javaProbe.setDataRate(new EveryNSeconds(5));
        hostMon.turnOnProbe(javaProbe);

    }

}