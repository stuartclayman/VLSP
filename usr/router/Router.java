package usr.router;

import java.util.List;

/**
 * A Router within UserSpaceRouting.
 */
public class Router {
    // The Router switching fabric
    RouterFabric fabric;

    // The Router controller
    RouterController controller;

    /**
     * Construct a Router.
     */
    public Router(int port) {
        controller = new RouterController(this, port);
        fabric = new SimpleRouterFabric(this);
    }

    /**
     * Construct a Router.
     */
    public Router(int mPort, int r2rPort) {
        controller = new RouterController(this, mPort, r2rPort);
        fabric = new SimpleRouterFabric(this);
    }


    /**
     * Start the router
     */
    public boolean start() {
        System.out.println("R: start");

        boolean controllerStart = controller.start();
        
        return controllerStart;
    }
        
    /**
     * Stop the router
     */
    public boolean stop() {
        controller.stop();

        System.out.println("R: stop");

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


}
