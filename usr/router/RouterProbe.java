// RouterProbe.java


package usr.router;

import eu.reservoir.monitoring.core.*;


/**
 * A probe that talks to a Router can collects the stats for thr Router.
 */
public abstract class RouterProbe extends AbstractProbe implements Probe {
    // The controller of the router we are getting stats for
    RouterController controller;


    /**
     * Get the RouterController.
     */
    public RouterController getController() {
        return controller;
    }

    /**
     * Set the RouterController.
     */
    public RouterProbe setController(RouterController cont) {
        controller = cont;
        return this;
    }

    /**
     * Last will get one last measurement
     */
    public void lastMeasurement() {
        //System.out.println("NetIFStatsProbe: last collect for " + controller.getName());
        try {
            Measurement m = collectThenCheck();
            getProbeManager().notifyMeasurement(m);
        } catch (Exception e) {
            //System.err.println("NetIFStatsProbe: last collect failed - " + e);
        }
    }

    /*
     * Lifecycle methods.
     * Can be overidden in subclasses to do something useful.
     */

    /**
     * Started
     */
    protected void started() {
    }

    /**
     * Stopped
     */
    protected void stopped() {
    }

    /**
     * Paused
     */
    protected void paused() {
    }

    /**
     * Resumed
     */
    protected void resumed() {
    }

}