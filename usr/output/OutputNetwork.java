package usr.output;

import java.io.PrintStream;
import usr.globalcontroller.GlobalController;
import usr.globalcontroller.visualization.*;

/** Class to output network stuff */
public class OutputNetwork implements OutputFunction {
  
    public void makeOutput(long time, PrintStream s, OutputType o, GlobalController gc) {
         //System.err.println("APS are "+APController_.getAPList());
        gc.APControllerUpdate(time);

        boolean printAP= o.getParameter().equals("AP");
        boolean printScore= o.getParameter().equals("Score");

        if (printAP) {
            networkWithCostGraphviz(s,gc);
            return;
        }

        if (printScore) {
            networkWithScoreGraphviz(s,gc);
            return;
        }

        plainNetworkGraphviz(s, gc);
    }

    /**
     * Send a network showing plain network as Graphviz to a PrintStream
     * showing the APs.
     */
    private void plainNetworkGraphviz(PrintStream s, GlobalController gc) {
        Visualization visualization = new ShowAPVisualization();

        visualization.setGlobalController(gc);
        visualization.visualize(s);
    }

    /**
     * Send a network showing AP costs as Graphviz to a PrintStream
     */
    private void networkWithCostGraphviz(PrintStream s, GlobalController gc) {
        Visualization visualization = new ShowAPCostsVisualization();

        visualization.setGlobalController(gc);
        visualization.visualize(s);

    }

    /**
     * Send a network showing AP costs as Graphviz to a PrintStream
     */
    private void networkWithScoreGraphviz(PrintStream s, GlobalController gc) {
        Visualization visualization = new ShowAPScoreVisualization();

        visualization.setGlobalController(gc);
        visualization.visualize(s);

    }

    /**
     * Send a network graph showing various attributes to a PrintStream.
     */
    public void visualizeNetworkGraph(String arg, PrintStream s, GlobalController gc) {
        if (!gc.isLatticeMonitoring()) {
            s.close();
        } else {
            // We might use arg one day.
            // Maybe to be a classname to instantiate.

            Visualization visualization = new usr.globalcontroller.visualization.ColouredNetworkVisualization();

            visualization.setGlobalController(gc);
            visualization.visualize(s);
        }
    }
  
}
