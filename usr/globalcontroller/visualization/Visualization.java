package usr.globalcontroller.visualization;

import usr.globalcontroller.GlobalController;
import java.io.PrintStream;

/**
 * An interface for all visualizers that will generate a visualization
 * of the current network topology.
 */
public interface Visualization {
    /**
     * Set the GlobalController this Visualization gets data from.
     */
    public void setGlobalController(GlobalController gc);

    /**
     * Visualize the current topology of the network.
     */
    public void visualize(PrintStream s);
}