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

    // a Map of thread id -> router
    private static HashMap<Long, Router> threadToRouter = new HashMap<Long, Router>();

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
     * Add thread id -> Router mapping info
     */
    static synchronized void addThreadContext(Long threadID, Router r) {
        System.err.println("Add thread " + threadID + " to " + r);
        threadToRouter.put(threadID, r);
    }

    /**
     * Remove thread id -> Router mapping info
     */
    static synchronized void removeThreadContext(Long threadID, Router r) {
        System.err.println("Remove thread " + threadID + " to " + r);
        threadToRouter.remove(threadID);
    }

    /**
     * Find a router by thread id
     */
    public static synchronized Router find(long id) {
        System.err.print("Finding thread " + id);

        Router r = threadToRouter.get(id);

        System.err.println(" found: " + r);

        return r;
    }

}
