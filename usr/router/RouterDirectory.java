package usr.router;

import usr.logging.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The RouterDirectory has a reference to the Router that
 * will be used by the DatagramSocket implementation
 * in order to decide which Router to connect new sockets to.
 */
public class RouterDirectory {
    // the list of Routers
    private static List<Router> routerList = new ArrayList<Router>();

    // a Map of thread group -> router
    private static HashMap<ThreadGroup, Router> threadToRouter = new HashMap<ThreadGroup, Router>();

    /**
     * Register a Router.
     */
    static synchronized void register(Router r) {
        routerList.add(r);
    }

    /**
     * Get the Router list.
     */
    public synchronized static List<Router> getRouterList() {
        return routerList;
    }

    /**
     * Add thread group -> Router mapping info
     */
    static synchronized void addThreadContext(ThreadGroup threadG, Router r) {
        System.err.println("Add thread group " + threadG + " to " + r);
        threadToRouter.put(threadG, r);
    }

    /**
     * Remove thread group -> Router mapping info
     */
    static synchronized void removeThreadContext(ThreadGroup threadG, Router r) {
        System.err.println("Remove thread group " + threadG + " to " + r);
        threadToRouter.remove(threadG);
    }

    /**
     * Find a router by thread group
     */
    public static synchronized Router find(ThreadGroup threadG) {
        System.err.print("Finding thread group " + threadG);

        Router r = threadToRouter.get(threadG);

        System.err.println(" found: " + r);

        return r;
    }

}
