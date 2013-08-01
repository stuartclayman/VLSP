package usr.applications;

import usr.logging.*;
import java.util.HashMap;

/**
 * A Thread Manager for USR Applications.
 */
class ApplicationThreadManager {
    // The ApplicationManager
    ApplicationManager appManager;

    // The threads we have.
    HashMap<String, Thread> threads;

    // Thread count
    int threadCount = 0;

    /**
     * Construct a ApplicationThreadManager
     */
    public ApplicationThreadManager(ApplicationManager manager) {
        appManager = manager;
        threads = new HashMap<String, Thread>();
    }

    /**
     * Execute an ApplicationHandle
     */
    public void execute(ApplicationHandle appH) {
        // create a thread name
        synchronized (threads) {
            String threadName = "/" + appManager.getRouter().getName() + "/" + appH.getApplication().getClass().getName() + "/" +
                threadCount;
            // set the thread name in the ApplicationHandle
            appH.setThreadName(threadName);

            // Allocate a Thread
            Thread t = new Thread(appH, threadName);

            // save it in the threads map
            threads.put(threadName, t);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting thread "  + threadName);

            // and start it
            t.start();

            threadCount++;
        }
    }

    /**
     * Cleanup a thread
     */
    public void waitFor(ApplicationHandle appH) {

        synchronized (threads) {
            String threadName = appH.getThreadName();

            // get the thread
            Thread t = threads.get(threadName);

            if (t == null) {
                // no thread for this app, so notihng to wait for
            } else {
                // there is a thread, so do a join
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Waiting for thread "  + threadName);

                try {
                    t.join();
                } catch (InterruptedException e) {
                }

                finally {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "End of "  + threadName);

                    // remove the thread from the threads map
                    threads.remove(threadName);
                }

            }
        }
    }

    String leadin() {
        return appManager.leadin();
    }

}