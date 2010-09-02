package usr.router;

import java.util.List;
import java.util.Scanner;

/**
 * A Router within UserSpaceRouting.
 */
public class Router {
    // The Router switching fabric
    RouterFabric fabric;

    // The Router controller
    RouterController controller;

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router 
     * connections.
     * @param port the port for the management console
     */
    public Router(int port) {
        controller = new RouterController(this, port);
        fabric = new SimpleRouterFabric(this);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router 
     * connections, plus a given name.
     * @param port the port for the management console
     * @param name the name of the router
     */
    public Router(int port, String name) {
        controller = new RouterController(this, port);
        fabric = new SimpleRouterFabric(this);
        setName(name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router 
     * connections.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     */
    public Router(int mPort, int r2rPort) {
        controller = new RouterController(this, mPort, r2rPort);
        fabric = new SimpleRouterFabric(this);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router 
     * connections, plus a given name.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     * @param name the name of the router
     */
    public Router(int mPort, int r2rPort, String name) {
        controller = new RouterController(this, mPort, r2rPort);
        fabric = new SimpleRouterFabric(this);
        setName(name);
    }


    /**
     * Start the router
     */
    public boolean start() {
        System.out.println(leadin() + "start");

        boolean controllerStart = controller.start();
        
        return controllerStart;
    }
        
    /**
     * Stop the router
     */
    public boolean stop() {
        controller.stop();

        System.out.println(leadin() + "stop");

        return true;
    }
        

    /**
     * Get the controller.
     */
    public RouterController getRouterController() {
        return controller;
    }

    /**
     * Get the fabric.
     */
    public RouterFabric getRouterFabric() {
        return fabric;
    }

    /**
     * Get the name of this Router.
     */
    public String getName() {
        return controller.name;
    }

    /**
     * Set the name of this RouterController.
     * This can only be done before the Router has started to
     * communicate with other elements.
     * @return false if the name cannot be set
     */
    public boolean setName(String name) {
        return controller.setName(name);
    }


    /**
     * Plug in a NetIF to the Router.
     */
    public RouterPort plugInNetIF(NetIF netIF) {
        return fabric.addNetIF(netIF);
    }

    /**
     * Get port N.
     */
    public RouterPort getPort(int p) {
        return fabric.getPort(p);
    }

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    public List<RouterPort> listPorts() {
        return fabric.listPorts();
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String R = "R: ";

        return getName() + " " + R;
    }

    public static void main(String[] args) {
        Router router = null;

        if (args.length == 1) {
            int mPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            router = new Router(mPort, "Router-" + mPort + "-" + (mPort+1));
        } else if (args.length == 2) {
            int mPort = 0;
            int r2rPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc = new Scanner(args[1]);
            r2rPort = sc.nextInt();
            router = new Router(mPort, r2rPort, "Router-" + mPort + "-" + r2rPort);
        } else if (args.length == 3) {
            int mPort = 0;
            int r2rPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc = new Scanner(args[1]);
            r2rPort = sc.nextInt();
            String name = args[2];

            router = new Router(mPort, r2rPort, name);
        } else {
            help();
        }

        // start
        if (router.start()) {
        } else {
            router.stop();
        }

    }

    private static void help() {
        System.err.println("Test1 [mgt_port [r2r_port]]");
        System.exit(1);
    }


}
