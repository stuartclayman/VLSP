// LocalControllerProbe.java


package usr.localcontroller;

import eu.reservoir.monitoring.core.AbstractProbe;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.Probe;


/**
 * A probe that talks to a LocalController can collects the stats for the LocalController
 */
public abstract class LocalControllerProbe extends AbstractProbe implements Probe {
    // The controller of the LocalController we are getting stats for
    LocalController controller;


    /**
     * Get the LocalController.
     */
    public LocalController getController() {
        return controller;
    }

    /**
     * Set the LocalController.
     */
    public LocalControllerProbe setController(LocalController cont) {
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
