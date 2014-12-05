package usr.router;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Future;

import usr.common.TimedThread;
import usr.common.SimpleThreadFactory;

import usr.interactor.RouterInteractor;
import usr.net.Address;


/**
 * Create a Router Environment.
 * It starts a router in its own ThreadGroup.
 */
public class RouterEnv {
    Router r;
    Starter starter;

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router
     * connections.
     * @param port the port for the management console
     */
    public RouterEnv(int port) {
        String name = "Router-" + port + "-" + (port+1);

        r = initRouter(port, port+1, name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on port+1 for the Router to Router
     * connections, plus a given name.
     * @param port the port for the management console
     * @param name the name of the router
     */
    public RouterEnv(int port, String name) {
        r = initRouter(port, port+1, name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router
     * connections.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     */
    public RouterEnv(int mPort, int r2rPort) {
        String name = "Router-" + mPort + "-" + r2rPort;

        r = initRouter(mPort, r2rPort, name);
    }

    /**
     * Construct a Router listening on a specified port for the
     * management console and on a specified for the Router to Router
     * connections, plus a given name.
     * @param mPort the port for the management console
     * @param r2rPort the port for Router to Router connections
     * @param name the name of the router
     */
    public RouterEnv(int mPort, int r2rPort, String name ) {
        r = initRouter(mPort, r2rPort, name);
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
    public RouterEnv(int mPort, int r2rPort, String name, Address address) {
        r = initRouter(mPort, r2rPort, name);
        r.setAddress(address);
    }

    /**
     * Stop the Router and end the Environment
     */
    public boolean stop() {
        return r.stop();
    }

    /**
     * Common initialisation section
     */
    private Router initRouter(int port1, int port2, String name) {
        // Create a Starter which is a Runnable
        // that has its own ThreadGroup
        starter = new Starter(port1, port2, name);

        // Execute the Starter
        ExecutorService executer = Executors.newSingleThreadExecutor(new SimpleThreadFactory(name));
        Future future = executer.submit(starter);

        // Get a handle on the Router itself
        Router r = getRouter();

        executer.shutdown();

        return r;
    }

    /**
     * Get the Router from Environment
     */
    public Router getRouter() {
        return starter.getRouter();
    }

    /**
     * Get a RouterInteractor into the Environment
     */
    public RouterInteractor getRouterInteractor() throws IOException {
        Router router = starter.getRouter();

        RouterInteractor interactor;

        try {
            interactor = new RouterInteractor("localhost", router.getManagementConsolePort());
            return interactor;

        } catch (UnknownHostException uhe) {
            // VERY BAD: cant resolve localhost - seems impossible
            throw new Error("System cannot resolve host: localhost");
        }

    }

    /**
     * Is the router active yet
     */
    public boolean isActive() {
        Router r = getRouter();
        return r.isActive();
    }

    /*
     * Set a shutdown hook which will
     * tidy up all the routers cleanly
     */
    static {
        Runtime.getRuntime().addShutdownHook(new TidyUp());
    }



}

/**
 * The Starter is a Runnable that creates a Router object
 * in its own ThreadGroup.
 * Using this enables each Router to be somewhat  independent
 * of the other Routers.
 */
class Starter implements Runnable {
    Object flag = new Object();
    boolean running = false;

    int port1;
    int port2;
    String name;
    Router router;

    /**
     * Construct a Starter
     */
    Starter(int port1, int port2, String name) {
        this.port1 = port1;
        this.port2 = port2;
        this.name = name;
    }

    // Thread.start jumps in here.
    @Override
    public void run() {
        System.out.println("Starting Router: " + name + " / " + port1 + " / " + port2);

        router = new Router(port1, port2, name);

        router.start();

        alive();

        /*
         * Maybe need to wait()
         * But so for it's not needed
         *
           // try {
           //  synchronized (this) {
           //      wait();
           //  }
           // } catch (InterruptedException ie) {
           // }
         */
    }

    /**
     * Notify that the Router is up and running and alive.
     */
    void alive() {
        synchronized (flag) {
            running = true;
            flag.notifyAll();
            System.out.println(name + " alive");
        }
    }

    /**
     * Get a handle on the Router.
     * This waits for it to be alive before it returns.
     */
    Router getRouter() {
        try {
            synchronized (flag) {
                //System.out.println(name + " running = " + running);

                if (!running) {
                    //System.out.println(name + " about to wait");
                    flag.wait();
                }
            }
        } catch (InterruptedException ie) {
            //System.out.println(name + " end of wait");
        }

        return router;
    }

}

/**
 * This class is used in the shutdown hook.
 * It tidies up all running routers.
 */
class TidyUp extends Thread {
    public TidyUp() {
    }

    @Override
    public void run() {
        List<Router> routers = RouterDirectory.getRouterList();

        for (Router router : routers) {
            if (router == null) {
                continue;
            }

            if (router.isActive()) {
                router.stop();
            }
        }
    }

}
