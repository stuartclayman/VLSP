package usr.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import usr.logging.Logger;
import usr.logging.USR;
import usr.common.ThreadTools;


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
        //Logger.getLogger("log").logln(USR.STDOUT, "Add thread group: " + threadG.getName() + " for router: " + r.getName());
        threadToRouter.put(threadG, r);

        //ThreadTools.findAllThreads(".. ");

    }

    /**
     * Remove thread group -> Router mapping info
     */
    static synchronized void removeThreadContext(ThreadGroup threadG, Router r) {
        //Logger.getLogger("log").logln(USR.STDOUT, "Remove thread group: " + threadG.getName() + " for router: " + r.getName());
        threadToRouter.remove(threadG);
    }

    /**
     * Find a router by thread group
     */
    public static synchronized Router find(ThreadGroup threadG) {
        //Logger.getLogger("log").logln(USR.STDOUT, "Finding thread group: " + threadG.getName());

        Router r = threadToRouter.get(threadG);

        //Logger.getLogger("log").logln(USR.STDOUT," found: " + r);

        if (r== null) {
            try {
                throw new Exception("RouterDirectory.find() FAILED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return r;
    }

    /**
     * Find a router by thread group
     */
    public static synchronized Router getRouter() {
        ThreadGroup threadG = Thread.currentThread().getThreadGroup();

        //Logger.getLogger("log").logln(USR.STDOUT,"Finding thread group: " + threadG.getName() + " for thread: " +
        // Thread.currentThread().getName());

        Router r = threadToRouter.get(threadG);

        while (r == null) {
         
            // try parent group
            threadG = threadG.getParent();

            if (threadG != null) {
                r = threadToRouter.get(threadG);
            } else {
                break;
            }


        }

        //Logger.getLogger("log").logln(USR.STDOUT," found: " + r);

        if (r== null) {
            try {
                //ThreadTools.findAllThreads(".. ");
                throw new Exception("RouterDirectory.getRouter() FAILED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return r;
    }

}
