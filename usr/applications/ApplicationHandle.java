package usr.applications;

import usr.logging.*;


/**
 * A handle on an Application.
 * It holds the name, the Application itself, and its state.
 */
public class ApplicationHandle implements Runnable {
    // The name
    String name;

    // The thread name
    String threadName;

    // The app
    Application app;

    // The state
    AppState state;

    /**
     * Construct an ApplicationHandle
     */
    ApplicationHandle(String name, Application app) {
        this.name = name;
        this.app = app;
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
     * Get the thread name
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Set the thread name
     */
    ApplicationHandle setThreadName(String name) {
        threadName = name;
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
            ApplicationManager.stopApp(getName());
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
