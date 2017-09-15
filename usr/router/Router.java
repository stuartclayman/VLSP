package usr.router;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

import usr.applications.ApplicationResponse;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import usr.common.Lifecycle;
import usr.net.Address;
import usr.net.AddressFactory;

/**
 * A Router within UserSpaceRouting.
 */
public class Router implements Lifecycle {
    /*
     * A Router is some glue that holds the RouterController
     * and the RouterFabric together.
     */

    // The Router switching fabric
    NetIFListener fabric;

    // Router options
    RouterOptions options_;
    // The Router controller
    RouterController controller;

    // Is the router active
    boolean isActive = false;

    // A ThreadGroup for all subthreads of this router.
    ThreadGroup threadGroup;

    // Stream for output
    FileOutputStream outputStream_ = null;

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router
     * connections.
     * @param port the port for the management console
     */
    Router(int port) {
        String name = "Router-" + port + "-" + (port+1);

        initRouter(port, port+1, name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router
     * connections, plus a given name.
     * @param port the port for the management console
     * @param name the name of the router
     */
    Router(int port, String name) {
        initRouter(port, port+1, name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router
     * connections.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     */
    Router(int mPort, int r2rPort) {
        String name = "Router-" + mPort + "-" + r2rPort;

        initRouter(mPort, r2rPort, name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router
     * connections, plus a given name.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     * @param name the name of the router
     */
    Router(int mPort, int r2rPort, String name ) {
        initRouter(mPort, r2rPort, name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router
     * connections, plus a given name.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     * @param name the name of the router
     * @param address the Address of the Router
     */
    Router(int mPort, int r2rPort, String name, Address address) {
        initRouter(mPort, r2rPort, name);

        setAddress(address);
    }

    /** Common initialisation section for all constructors */
    void initRouter(int port1, int port2, String name) {
        // allocate a new logger
        Logger logger = Logger.getLogger("log");
        // tell it to output to stdout
        // and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.STDOUT set
        logger.addOutput(System.out, new BitMask(USR.STDOUT));
        // tell it to output to stderr
        // and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.ERROR set
        logger.addOutput(System.err, new BitMask(USR.ERROR));

        // add some extra output channels, using mask bit 6
        try {
            logger.addOutput(new PrintWriter(new FileOutputStream("/tmp/" + name + "-channel6.out")), new BitMask(1<<6));
            //logger.addOutput(System.out, new BitMask(1<<6));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get current ThreadGroup
        threadGroup = Thread.currentThread().getThreadGroup();

        options_ = new RouterOptions(this);

        controller = new RouterController(this, options_, port1, port2, name);

        init();

    }

    /**
     * init the router
     */
     public boolean init() {

        fabric = new SimpleRouterFabric(this, options_);
        //fabric = new VectorRouterFabric(this, options_);

        if (!fabric.init()) {
            throw new Error("RouterFabric failed to init()");
        }

        RouterDirectory.register(this);

        return true;

    }

    /**
     * Start the router
     */
    public boolean start() {
        if (!isActive) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

            boolean fabricStart = fabric.start();
            boolean controllerStart = controller.start();

            // if the fabric and the controller started OK
            if (fabricStart && controllerStart) {

                addThreadContext(threadGroup);

                isActive = true;

                return true;
            } else {
                // stop anything that started
                fabric.stop();

                controller.stop();

                return false;
            }

        } else {
            return false;
        }
    }

    /**
     * Stop the router --- called from internal threads such as management console
     */
    public boolean stop() {
        if (isActive) {
            isActive = false;
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");

            controller.stop();
            fabric.stop();

            removeThreadContext(threadGroup);

            try {
                if (outputStream_ != null) {
                    outputStream_.close();
                }
            } catch (java.io.IOException ex) {
            }

            return true;
        } else {
            return false;
        }
    }

    /** Send goodbye message to all interfaces */
    public void sendGoodbye() {
        fabric.sendGoodbye();
    }

    public void shutDown() {
        stop();
    }

    /**
     * Is the router active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Get the Thread Group.
     */
    ThreadGroup getThreadGroup() {
        return threadGroup;
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
     * Get the AppSockMux this talks to.
     */
    AppSocketMux getAppSocketMux() {
        return controller.getAppSocketMux();
    }

    /**
     * Start an App.
     * It takes a class name and some args.
     * It returns app_name ~= /Router-1/App/class.blah.Blah/1
     */
    public ApplicationResponse appStart(String commandstr) {
        return controller.appStart(commandstr);
    }

    /**
     * Stop an App.
     * It takes an app name
     */
    public ApplicationResponse appStop(String commandstr) {
        return controller.appStop(commandstr);
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
     * Get the router address.
     * This is a special feature where the router itself has its own address.
     */
    public Address getAddress() {
        return controller.getAddress();
    }

    /**
     * Set the router address.
     * This is a special feature where the router itself has its own address.
     */
    public boolean setAddress(Address addr) {
        return controller.setAddress(addr);
    }

    /** get listener */
    public NetIFListener getListener() {
        return fabric;
    }

    /**
     * Get the routing table
     */
    public RoutingTable getRoutingTable() {
        return fabric.getRoutingTable();
    }

    /**
     * List NetIFs
     */
    public List<NetIF> listNetIF() {
        return fabric.listNetIF();
    }

    /**
     * Plug in a NetIF to the Router.
     */
    public RouterPort plugInNetIF(NetIF netIF) {
        return controller.plugInNetIF(netIF);
    }

    /**
     * Get the port for the ManagementConsole.
     */
    public int getManagementConsolePort() {
        return controller.getManagementConsolePort();
    }

    /**
     * Get the port for the connection port
     */
    public int getConnectionPort() {
        return controller.getConnectionPort();
    }

    /** Try to ping router with a given id */
    public boolean ping(Address addr) {
        return fabric.ping(addr);
    }

    /** Try to echo to a router with a given id */
    public boolean echo(Address addr) {
        return fabric.echo(addr);
    }

    /**
     * Find a NetIF by name.
     */
    public NetIF findNetIF(String name) {
        return fabric.findNetIF(name);
    }

    /**
     * Set the netIF weight associated with a link to a certain router name
     */
    public boolean setNetIFWeight(String name, int weight) {
        return fabric.setNetIFWeight(name, weight);
    }

    /**
     * Get the local NetIF that has the sockets.
     */
    public NetIF getLocalNetIF() {
        return fabric.getLocalNetIF();
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
     * Close port.
     */
    public void closePort(RouterPort port) {
        fabric.closePort(port);
    }

    /**
     * Add router thread context info.
     */
    public void addThreadContext(ThreadGroup threadG) {
        RouterDirectory.addThreadContext(threadG, this);
    }

    /**
     * Remove router thread context info.
     */
    public void removeThreadContext(ThreadGroup threadG) {
        RouterDirectory.removeThreadContext(threadG, this);
    }

    /** Remove a network interface from the router */
    public void removeNetIF(NetIF n) {
        fabric.removeNetIF(n);
    }

    /** Read a string containing router options */
    boolean readOptionsString(String str) {
        Logger logger = Logger.getLogger("log");
        try {
            //Logger.getLogger("log").logln(USR.ERROR, "TRYING TO PARSE STRING "+str);
            options_.setOptionsFromString(str);

        } catch (Exception e) {
            logger.logln(USR.ERROR, "Cannot parse options string");
            logger.logln(USR.ERROR, e.getMessage());
            return false;
        }

        String fileName = options_.getOutputFile();

        if (!fileName.equals("")) {
            if (options_.getOutputFileAddName()) {
                fileName += "_"+leadinFname();
            }
            File output = new File(fileName);
            try {
                outputStream_ = new FileOutputStream(output);
                PrintWriter pw = new PrintWriter(outputStream_, true);
                logger.removeOutput(System.out);
                logger.addOutput(pw, new BitMask(USR.STDOUT));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.exit(-1);
            }
        }

        String errorName = options_.getErrorFile();

        if (!errorName.equals("")) {
            if (options_.getOutputFileAddName()) {
                errorName += "_"+leadinFname();
            }
            File output = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output);
                PrintWriter pw = new PrintWriter(fos, true);
                logger.removeOutput(System.err);
                logger.addOutput(pw, new BitMask(USR.ERROR));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.exit(-1);
            }
        }
        return true;
    }

    /** Read a file containing router options */

    boolean readOptionsFile(String fName) {
        Logger logger = Logger.getLogger("log");
        try {
            options_.setOptionsFromFile(fName);

        } catch (Exception e) {
            logger.logln(USR.ERROR, "Cannot parse options file");
            logger.logln(USR.ERROR, e.getMessage());

            return false;
        }

        String fileName = options_.getOutputFile();

        if (!fileName.equals("")) {
            if (options_.getOutputFileAddName()) {
                fileName += "_"+leadinFname();
            }
            File output = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output, true);
                PrintWriter pw = new PrintWriter(fos, true);
                logger.removeOutput(System.out);
                logger.addOutput(pw, new BitMask(USR.STDOUT));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.exit(-1);
            }
        }
        String errorName = options_.getErrorFile();

        if (!errorName.equals("")) {
            if (options_.getOutputFileAddName()) {
                errorName += "_"+leadinFname();
            }
            File output = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(output, true);
                PrintWriter pw = new PrintWriter(fos, true);
                logger.removeOutput(System.err);
                logger.addOutput(pw, new BitMask(USR.ERROR));
            } catch (Exception e) {
                System.err.println("Cannot output to file");
                System.exit(-1);
            }
        }
        return true;
    }

    /**
     * Main entry point
     */
    public static void main(String[] argss) {
        final String[] args = argss;

        @SuppressWarnings("unused")
        RouterEnv router = null;

        if (args.length == 1) {
            int mPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            router = new RouterEnv(mPort, "Router-" + mPort + "-" + (mPort+1));
        } else if (args.length == 2) {
            int mPort = 0;
            int r2rPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            sc = new Scanner(args[1]);
            r2rPort = sc.nextInt();
            sc.close();
            router = new RouterEnv(mPort, r2rPort, "Router-" + mPort + "-" + r2rPort);
        } else if (args.length == 3) {
            int mPort = 0;
            int r2rPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            sc = new Scanner(args[1]);
            r2rPort = sc.nextInt();
            sc.close();
            String name = args[2];
            Address addr = null;
            try {
                addr = AddressFactory.newAddress(args[2]);
            } catch (java.net.UnknownHostException e) {
            }

            if (addr == null) {
                router = new RouterEnv(mPort, r2rPort, name);
            } else {
                router = new RouterEnv(mPort, r2rPort, name, addr);
            }
        } else if (args.length == 4) {
            int mPort = 0;
            int r2rPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            sc = new Scanner(args[1]);
            r2rPort = sc.nextInt();
            sc.close();
            String name = args[2];
            Address addr = null;
            try {
                addr = AddressFactory.newAddress(args[3]);
            } catch (java.net.UnknownHostException e) {
                System.err.println("Cannot construct address from "+args[3]);
                help();
            }

            router = new RouterEnv(mPort, r2rPort, name, addr);
        } else {
            help();
        }

    }

    private static void help() {
        Logger.getLogger("log").logln(USR.ERROR, "Router [mgt_port [r2r_port]] [name] [address]");
        System.exit(1);
    }

    String leadinFname() {
        return "R_"+getName();
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String R = "R: ";

        return getName() + " " + R;
    }

}
