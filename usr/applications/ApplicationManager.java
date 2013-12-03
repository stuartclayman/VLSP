package usr.applications;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import usr.logging.Logger;
import usr.logging.USR;
import usr.router.Router;

/**
 * The ApplicationManager is reponsible for starting and stopping
 * applications.
 */
public class ApplicationManager {
    // The router this is an ApplicationManager for.
    Router router;

    // A pool
    ApplicationThreadManager pool;

    // A map of all the Applications, name -> ApplicationHandle object
    HashMap<String, ApplicationHandle> appMap;

    // An App ID / PID
    int appID = 0;

    /**
     * ApplicationManager Constructor.
     */
    public ApplicationManager(Router router) {
        // create an pool
        pool = new ApplicationThreadManager(this);

        appMap = new HashMap<String, ApplicationHandle>();
        this.router = router;
    }

    /**
     * Static entry point to start an Application.
     * Returns an ApplicationResponse with an app name in it.
     * The appName is used to stop the app, and is of the form
     * /Router-1/App/plugins_usr.aggregator.appl.InfoSource/2.
     */
    public synchronized ApplicationResponse startApp(String className, String[] args) {
        // args should be class name + args for class

        ApplicationResponse result = execute(className, args);

        return result;
    }

    /**
     * Static entry point to stop an Application.
     * Application name is passed in, in the form
     * /Router-1/App/plugins_usr.aggregator.appl.InfoSource/2
     */
    public synchronized ApplicationResponse stopApp(String appName) {
        return terminate(appName);
    }

    /**
     * Static entry point to stop all Application.
     * Application name is passed in.
     */
    public synchronized void stopAll() {
        //System.err.println("Stopping all apps");
        shutdown();
        //System.err.println("Stopped all apps");
    }

    /**
     * Static entry point to list Applications.
     */
    public synchronized Collection<ApplicationHandle> listApps() {
        return appMap.values();
    }

    /**
     * Get an ApplicationHandle for an Application.
     */
    public synchronized ApplicationHandle find(String name) {
        return appMap.get(name);
    }

    /**
     * Execute an object with ClassName and args
     */
    private synchronized ApplicationResponse execute(String className, String[] args) {

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "execute: " + className + " args: " + Arrays.asList(args));

        // Class
        Class<?> clazz;
        Constructor<? extends Application> cons0;

        try {
            // get Class object
            clazz = Class.forName(className);

            // check if the class implements the right interface
            // it is an Application
            try {
                Class<? extends Application> appClazz = clazz.asSubclass(Application.class );
                cons0 = appClazz.getDeclaredConstructor();
            } catch (ClassCastException cce) {
                // it is not an Application, so we cant run it
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "class " + className + " is not at Application");
                return new ApplicationResponse(false, "class " + className + " is not at Application");
            }

            // create an instance of the Application
            Application app = cons0.newInstance();

            appID++;

            // set app name
            String appName = "/" + router.getName() + "/App/" + className + "/" + appID;

            // initialize it
            ApplicationResponse initR = null;


            synchronized (app) {
                initR = app.init(args);
            }

            if (initR == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Application: " + appName + " failed to init");
                return new ApplicationResponse(false, "class " + className + " Application failed to init");
            }

            // if init fails, return
            if (!initR.isSuccess()) {
                return initR;
            }

            // otherwise create an ApplicationHandle for the app
            ApplicationHandle handle = new ApplicationHandle(this, appName, app, args, appID);

            // try and start the app
            ApplicationResponse startR;

            synchronized (app) {
                startR = app.start();
            }

            //  check if startR is null
            if (startR == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Application: " + handle + " failed to start");
                return new ApplicationResponse(false, "class " + className + " Application failed to start");
            }

            // if start succeeded then go onto run()
            if (startR.isSuccess()) {
                // now add details to Application map
                appMap.put(appName, handle);

                handle.setState(ApplicationHandle.AppState.RUNNING);

                pool.execute(handle);

                // Logger.getLogger("log").logln(USR.ERROR, leadin() + "pool = " + pool.getActiveCount() + "/" + pool.getTaskCount()
                // + "/" + pool.getPoolSize());

                return new ApplicationResponse(true, appName);

            } else {
                // the app did not start properly
                handle.setState(ApplicationHandle.AppState.STOPPED);

                return startR;
            }

        } catch (ClassNotFoundException cnfe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "ClassNotFoundException " + cnfe);
            return new ApplicationResponse(false, "ClassNotFoundException " + cnfe);

        } catch (NoClassDefFoundError ncdfe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "NoClassDefFoundError " + ncdfe);
            return new ApplicationResponse(false, "NoClassDefFoundError " + ncdfe);

        } catch (NoSuchMethodException nsme) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "NoSuchMethodException " + nsme);
            return new ApplicationResponse(false, "NoSuchMethodException " + nsme);

        } catch (InstantiationException ie) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "InstantiationException " + ie);
            return new ApplicationResponse(false, "InstantiationException " + ie);

        } catch (IllegalAccessException iae) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "IllegalAccessException " + iae);
            return new ApplicationResponse(false, "IllegalAccessException " + iae);

        } catch (InvocationTargetException ite) {
            Throwable t = ite.getCause();

            Logger.getLogger("log").logln(USR.ERROR, leadin() + "InvocationTargetException -> Throwable " + t);
            return new ApplicationResponse(false, "InvocationTargetException for " + t);
        }

    }

    /**
     * Stop an Application
     */
    private ApplicationResponse terminate(String appName) {
        ApplicationHandle appH = appMap.get(appName);

        if (appH == null) {
            // no app with that name
            return new ApplicationResponse(false, "No Application called " + appName);
        } else {
            synchronized (appH) {  // This must not be called twice for the same appH simultaneously

                if (appH.getState() == ApplicationHandle.AppState.STOPPED) {

                    // wait for the thread to actually end
                    //pool.waitFor(appH);

                    // and remove from the app map
                    appMap.remove(appName);

                    return new ApplicationResponse(false, "Application called " + appName + " already stopped");
                } else {
                    // NOT ApplicationHandle.AppState.STOPPED
                    try {

                        Application app = appH.getApplication();
                        // try and stop the app
                        ApplicationResponse stopR = null;

                        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stopping " + appName);

                        if (appH.getState() == ApplicationHandle.AppState.RUNNING) {
                            appH.setState(ApplicationHandle.AppState.STOPPING);
                            synchronized (app) {
                                stopR = app.stop();
                            }


                            // wait for the thread to actually end
                            pool.waitFor(appH);
                        } else if (appH.getState() == ApplicationHandle.AppState.APP_POST_RUN) {
                            // the app had already exited the run loop
                            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Cleanup app after exiting run() " + appName);
                            synchronized (app) {
                                stopR = app.stop();
                            }

                        }


                        // stop state
                        appH.setState(ApplicationHandle.AppState.STOPPED);

                        // and remove from the app map
                        appMap.remove(appName);

                        //  check if stopR is null
                        if (stopR == null) {
                            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Application: " + appH + " failed to stop");
                            return new ApplicationResponse(false,
                                                           "Application called " + appName + " failed to stop - returned null.");
                        } else {
                            return new ApplicationResponse(true, "");
                        }

                    } catch (Exception e) {
                        return new ApplicationResponse(false,
                                                       "Application called " + appName + " failed to stop with Exception " +
                                                       e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Shutdown the ApplicationManager
     */
    private synchronized void shutdown() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "shutdown ");

        Collection<ApplicationHandle> apps = new java.util.LinkedList<ApplicationHandle>(appMap.values());

        for (ApplicationHandle appH : apps) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Attempting to terminate "+appH);
            terminate(appH.getName());
        }

        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "pool shutdown ");
        //pool.shutdown();
    }

    /**
     * Get the Router this is an ApplicationManager for.
     */
    public Router getRouter() {
        return router;
    }

    String leadin() {
        final String AM = "AppM: ";

        return router.getName() + " " + AM;
    }

}