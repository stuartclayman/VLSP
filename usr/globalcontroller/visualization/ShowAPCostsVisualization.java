package usr.globalcontroller.visualization;

import java.io.PrintStream;

import usr.globalcontroller.GlobalController;

/**
 * A view of the current network topology, showing where aggregation points are.
 */
public class ShowAPCostsVisualization implements Visualization {
    GlobalController gc;

    public ShowAPCostsVisualization() {
    }

    /**
     * Set the GlobalController this Visualization gets data from.
     */
    @Override
	public void setGlobalController(GlobalController gc) {
        this.gc = gc;
    }

    /**
     * Visualize the current topology of the network.
     */
    @Override
	public void visualize(PrintStream s) {
        s.println("Graph G {");

        for (int r : gc.getRouterList()) {
            int ap = gc.getAPController().getAP(r);

            if (ap == r) {
                s.print(r+" [shape=box");
            } else {
                s.print(r+" [shape=circle");
            }

            s.print(",label=\""+ap+" (" + gc.getAPController().getAPCost(r) + ")\"");

            s.println("];");
        }

        for (int i : gc.getRouterList()) {
            for (int j : gc.getOutLinks(i)) {
                if (i < j) {
                    s.println(i+ " -- "+j+";");
                }
            }
        }
        s.println("}");


    }

}