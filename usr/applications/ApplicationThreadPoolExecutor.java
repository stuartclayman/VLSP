package usr.applications;

import usr.logging.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.SynchronousQueue;


/**
 * A Thread Pool for Applications.
 * It runs special methods before and after each
 * Application is executed.
 */
class ApplicationThreadPoolExecutor extends ThreadPoolExecutor {
    // ApplicationManager
    ApplicationManager manager;

    /**
     * Creates a new ThreadPoolExecutor with the given initial
     * parameters and default thread factory and handler.
     */
    ApplicationThreadPoolExecutor(ApplicationManager manager) {
        // same args as ExecutorService.newCachedThreadPool()
        super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, 
              new SynchronousQueue<Runnable>());

        this.manager = manager;
    }
              
    /**
     * Method invoked prior to executing the given Runnable in the given thread.
     * Called just before Application is executed.
     */
    protected void beforeExecute(Thread t, Runnable r) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "beforeExecute: " + t + " => " + r);

        super.beforeExecute(t, r);

        ApplicationHandle handle = (ApplicationHandle)r;
        Application app = handle.getApplication();

        // start up sequence
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "starting " + handle.getName());

        ApplicationResponse startR = app.start();

        // if start succeeded then go onto run()
        if (startR.isSuccess()) {
            handle.setState(ApplicationHandle.AppState.RUNNING);
        } else {
            handle.setState(ApplicationHandle.AppState.STOPPED);
        }
    }

    /**
     * Method invoked upon completion of execution of the given Runnable.
     */
    protected void afterExecute(Runnable r, Throwable t) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "afterExecute: " + t + " => " + r);

        super.afterExecute(r, t);

        ApplicationHandle handle = (ApplicationHandle)r;
        Application app = handle.getApplication();

        if (handle.getState() == ApplicationHandle.AppState.RUNNING) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stopping " + handle.getName());
            app.stop();
            handle.setState(ApplicationHandle.AppState.STOPPED);
        }


        manager.terminate(handle.getName());
    }

    String leadin() {
        final String AP = "AP: ";

        return usr.router.RouterDirectory.getRouter().getName() + " " + AP;
    }


}
