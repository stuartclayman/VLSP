package usr.router;

import java.util.List;
import usr.logging.*;
import java.util.ArrayList;

/**
 * The RouterDirectory has a reference to the Router that
 * will be used by the DatagramSocket implementation
 * in order to decide which Router to connect new sockets to.
 */
public class RouterDirectory {
    // the instance
    private static Router theInstance;

    // the list of Routers
    private static List<Router> routerList = new ArrayList<Router>();

    /**
     * Get the Router.
     */
    public synchronized static Router getRouter() {
	return theInstance;
    }

    /**
     * Set the instance.
     */
    static synchronized void setInstance(Router r) {
	theInstance = r;
    }

    /**
     * Register a Router.
     */
    static synchronized void register(Router r) {
	routerList.add(r);
	setInstance(r);
    }

}
