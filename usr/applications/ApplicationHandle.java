package usr.applications;

import usr.logging.*;


/**
 * A handle on an Application.
 * It holds the name, the Application itself, and its state.
 */
public class ApplicationHandle implements Runnable {
    // The name
    String name;

    // The app
    Application app;

    // The Thread
    Thread thread;

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
     * Get the thread
     */
    public Thread getThread() {
        return thread;
    }

    /**
     * Set the thread
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
    public void run() {
        Logger.getLogger("log").logln(USR.STDOUT, "entering run: " + app);

        if (getState() == ApplicationHandle.AppState.RUNNING) {
            app.run();
        }

        Logger.getLogger("log").logln(USR.STDOUT, "exiting run: " + app); 
    }

    /**
     * The states of the app
     */
    public enum AppState {
        APP_POST_INIT,   // after for init()
        RUNNING,         // we have entered run()
        STOPPED          // we have called stop() and the the app should stop
    }



}
