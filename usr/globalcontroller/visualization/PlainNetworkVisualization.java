package usr.globalcontroller.visualization;

import usr.globalcontroller.GlobalController;
import java.io.PrintStream;

/**
 * A plain view of the current network topology.
 */
public class PlainNetworkVisualization implements Visualization {
    GlobalController gc;

    public PlainNetworkVisualization() {
    }

    /**
     * Set the GlobalController this Visualization gets data from.
     */
    public void setGlobalController(GlobalController gc) {
	this.gc = gc;
    }

    /**
     * Visualize the current topology of the network.
     */
    public void visualize(PrintStream s) {
	s.println("Graph G {");
	for (int r : gc.getRouterList()) {
	    s.print(r+" [shape=circle");

	    s.println("];");
	}

	for (int i : gc.getRouterList()) {
	    for (int j : gc.getOutLinks(i)) {
		if (i < j)
		    s.println(i+ " -- "+j+";");
	    }
	}
	s.println("}");


    }
}

