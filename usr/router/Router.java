package usr.router;

import usr.net.Datagram;
import usr.logging.*;
import usr.net.DatagramFactory;
import usr.protocol.Protocol;
import java.util.List;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.io.*;
import usr.net.GIDAddress;
import usr.applications.*;
import java.util.*;

/**
 * A Router within UserSpaceRouting.
 */
public class Router {
    /*
     * A Router is some glue that holds the RouterController
     * and the RouterFabric together.
     */

    // The Router switching fabric
    RouterFabric fabric;

    // Router options
    RouterOptions options_;

    // The Router controller
    RouterController controller;

    // ApplicationSocket Multiplexor
    AppSocketMux appSocketMux;

    // Is the router active
    boolean isActive = false;

    ArrayList <Application> appList= null;

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router 
     * connections.
     * @param port the port for the management console
     */
    public Router(int port) {
        initRouter(port, port+1);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router 
     * connections, plus a given name.
     * @param port the port for the management console
     * @param name the name of the router
     */
    public Router(int port, String name) {
        initRouter(port, port+1);


        setName(name);
        try {
          int gid= Integer.parseInt(name);
          setGlobalID(gid);
        } catch (Exception e) {
        
        }
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router 
     * connections.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     */
    public Router(int mPort, int r2rPort) {
        initRouter(mPort, r2rPort);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router 
     * connections, plus a given name.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     * @param name the name of the router
     */
    public Router(int mPort, int r2rPort, String name ) {
        initRouter(mPort, r2rPort);

        setName(name);
        try {
          int gid= Integer.parseInt(name);
          setGlobalID(gid);
        } catch (Exception e) {
        
        }
    }
    
 
    /** Common initialisation section for all constructors */
    void initRouter(int port1, int port2) 
    
    {
        controller = new RouterController(this, port1, port2);
        options_= new RouterOptions(this);
        fabric = new SimpleRouterFabric(this, options_);
        RouterDirectory.register(this);
        appList= new ArrayList<Application>();

        // allocate a new logger
        Logger logger = Logger.getLogger("log");
        // tell it to output to stdout
        // and tell it what to pick up
        // it will actually output things where the log has bit 1 set
        logger.addOutput(System.out, new BitMask(USR.STDOUT));
        // tell it to output to stderr
        // and tell it what to pick up
        // it will actually output things where the log has bit 2 set
        logger.addOutput(System.err, new BitMask(USR.ERROR));

    }
    /**
     * Get the router address.
     * This is a special featrue for GID.
     */
    public GIDAddress getAddress() {
        return controller.getAddress();
    }

    /**
     * Start the router
     */
    public boolean start() {
        if (!isActive) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

            boolean fabricStart = fabric.start();
            boolean controllerStart = controller.start();

            appSocketMux = new AppSocketMux(controller);
            appSocketMux.start();
      
            isActive = true;

            return controllerStart && fabricStart;
        } else {
            return false;
        }
    }
        
    /**
     * Stop the router
     */
    public boolean stop() {
        if (isActive) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");

            appSocketMux.stop();

            controller.stop();
            fabric.stop();

            stopApplications();

            isActive = false;

            return true;
        } else {
            return false;
        }
    }

    /**
     * Is the router active
     */
    public boolean isActive() {
        return isActive;
    }

    /** Stop running applications if any */
    void stopApplications () {
        for (Application a: appList) {
            a.stop();
        }
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
        return appSocketMux;
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
     * Get the global ID of this Router.
     */
    public int getGlobalID() {
        return controller.getGlobalID();
    }

    /**
     * Set the globalID of this Router.
     */
    public boolean setGlobalID(int id) {
        return controller.setGlobalID(id);
    }

    /** 
     * Get the routing table
     */
    public RoutingTable getRoutingTable() {
        return fabric.getRoutingTable();
    }


    /**
     * Plug in a NetIF to the Router.
     */
    public RouterPort plugInNetIF(NetIF netIF) {
        return fabric.addNetIF(netIF);
    }

        /** Try to ping router with a given id */
    public boolean ping(int id){
        return fabric.ping(id);
    }


    /** Try to echo to a router with a given id */
    public boolean echo(int id){
        return fabric.echo(id);
    }

    /**
     * Find a NetIF by name.
     */
    public NetIF findNetIF(String name) {
        return fabric.findNetIF(name);
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
     * Create the String to print out before a message
     */
    String leadin() {
        final String R = "R: ";

        return getName() + " " + R;
    }

     public void pingNeighbours() 
    {
       
        byte []b= new byte[1];
        b[0]='P';
        //Logger.getLogger("log").logln(USR.ERROR, "Pinging");
        Datagram dg= DatagramFactory.newDatagram(Protocol.CONTROL, 
            ByteBuffer.wrap(b));
        List<NetIF> nif= listNetIF();
        if (nif == null)
            return;
        //Logger.getLogger("log").logln(USR.ERROR, "COLLECTION IS  "+nif);
        for (NetIF n : nif) {
            
            if (n.sendDatagram(dg) == true) {
                Logger.getLogger("log").logln(USR.ERROR, "Ping sent");
            }
        }
    }
    
    /** Remove a network interface from the router */
    public void removeNetIF(NetIF n) {
       fabric.removeNetIF(n);
    }

    
        /** Read a string containing router options */
    public boolean readOptionsString(String str) 
    {
        try { 
            //Logger.getLogger("log").logln(USR.ERROR, "TRYING TO PARSE STRING "+str);
            options_.setOptionsFromString(str);
            return true;
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot parse options string");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return false;
        }
    }
    
    public List<NetIF> listNetIF() {
        return fabric.listNetIF();
    }
   
    /** Read a file containing router options */
    
    public boolean readOptionsFile(String fName)
    {
        try { 
            options_.setOptionsFromFile(fName);
            return true;
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot parse options file");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return false;
        }    
    }

    /** Try to run a command (implementing class Application on the given
    router */
    public synchronized boolean runCommand(String command, String args)
    {
        Application app= null;
        if (command.equals("PING")) {
            try {
                app= new PingApplication(this, args);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());
                return false;
            }
        }
        if (app == null) {
            return false;
        }
        Thread thread = new Thread(app);
        return app.start(); 
        
    }
    
    /** Application stops running */
    public synchronized void commandExit(Application app)
    {
        for (Application a: appList) {
            if (app.equals(a)) {
                appList.remove(a);
                return;
            }
        }
        Logger.getLogger("log").logln(USR.ERROR, leadin()+"Exiting application appears to to be on app list");
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
        } 
        
        else {
            help();
        }

        // start
        if (router.start()) {
        } else {
            router.stop();
        }

    }


    private static void help() {
        Logger.getLogger("log").logln(USR.ERROR, "Test1 [mgt_port [r2r_port]]");
        System.exit(1);
    }

    static {
        Runtime.getRuntime().addShutdownHook(new TidyUp());
    }

}

class TidyUp extends Thread {
    public TidyUp() {
    }

    public void run() {
        Router router = RouterDirectory.getRouter();

        if (router.isActive()) {
            router.stop();
        }
    }
}
