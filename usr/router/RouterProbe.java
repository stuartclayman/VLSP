// RouterProbe.java


package usr.router;

import eu.reservoir.monitoring.core.AbstractProbe;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.Timestamp;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.ProbeManager;


/**
 * A probe that talks to a Router can collects the stats for thr Router.
 */
public abstract class RouterProbe extends AbstractProbe implements Probe {
    // The controller of the router we are getting stats for
    RouterController controller;

    Timestamp startTime;

    RouterProbe() {
        startTime = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Get the start time of this probe
     */
    public Timestamp getStartTime() {
        return startTime;
    }

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
            ProbeManager pm = getProbeManager();

            if (pm.isProbeOn(this)) {
                int result = pm.notifyMeasurement(m);

                if (result == 0) {
                    // Measurement was not queued - probably shutting down
                    System.err.println("NetIFStatsProbe: queued Measurement failed");
                }
            } else {
                System.err.println("NetIFStatsProbe: Measurement not queued - probe NOT on");
            }
            
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
