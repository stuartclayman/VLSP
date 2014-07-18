package usr.applications;

import usr.logging.Logger;
import usr.logging.USR;


/**
 * A handle on an Application.
 * It holds the name, the Application itself, and its state.
 */
public class ApplicationHandle implements Runnable {
    // The name
    String name;

    // The thread 
    Thread thread;

    // The app
    Application app;

    // The args
    String[] args;

    // The app ID
    int appID;

    // Start Time
    long startTime;

    // The state
    AppState state;

    // The ApplicationManager
    ApplicationManager manager;

    /**
     * Construct an ApplicationHandle
     */
    ApplicationHandle(ApplicationManager appMgr, String name, Application app, String[] args, int appID) {
        this.name = name;
        this.app = app;
        this.args = args;
        this.appID = appID;
        this.manager = appMgr;
        this.startTime = System.currentTimeMillis();
        setState(AppState.APP_POST_INIT);
    }

    /**
     * Get the Application name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Application
     */
    public Application getApplication() {
        return app;
    }

    /**
     * Get the args for the App.
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * Get the App ID.
     */
    public int getID() {
        return appID;
    }

    /**
     * Get the start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the thread name
     */
    public String getThreadName() {
        return thread.getName();
    }

    /**
     * Get the thread
     */
    public Thread getThread() {
        return thread;
    }

    /**
     * Set the thread name
     */
    ApplicationHandle setThread(Thread t) {
        thread = t;
        return this;
    }

    /**
     * Get the state
     */
    public AppState getState() {
        return state;
    }

    /**
     * Set the state
     */
    ApplicationHandle setState(AppState s) {
        state = s;
        return this;
    }

    /**
     * This run() delegates to Application run()
     */
    @Override
	public void run() {
        Logger.getLogger("log").logln(USR.STDOUT, "ApplicationHandle: entering run: " + app);

        if (getState() == ApplicationHandle.AppState.RUNNING) {
            app.run();
        }

        Logger.getLogger("log").logln(USR.STDOUT, "ApplicationHandle: exiting run: " + app + " with state of: " + getState());

        // if we get to the end of run() and the app
        // is still in the RUNNING state,
        // we need to stop it
        if (getState() == ApplicationHandle.AppState.RUNNING) {
            setState(ApplicationHandle.AppState.APP_POST_RUN);
            manager.stopApp(getName());
        }

    }

    /**
     * The states of the app
     */
    public enum AppState {
        APP_POST_INIT,   // after for init()
        RUNNING,         // we have entered run()
        APP_POST_RUN,    // the app dropped out of run(), without a stop()
        STOPPING,          // we have called stop() and the the app should stop
        STOPPED          // the app is stopped
    }



}
