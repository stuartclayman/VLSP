package usr.output;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.globalcontroller.GlobalController;
import usr.globalcontroller.visualization.ShowAPCostsVisualization;
import usr.globalcontroller.visualization.ShowAPScoreVisualization;
import usr.globalcontroller.visualization.ShowAPVisualization;
import usr.globalcontroller.visualization.Visualization;

/** Class to output network stuff */
public class OutputNetwork implements OutputFunction {

    @Override
    public void makeOutput(long time, PrintStream s, OutputType o, GlobalController gc) {
        //System.err.println("APS are "+APController_.getAPList());
        // gc.APControllerUpdate(time);

        boolean printAP = o.getParameter().equals("AP");
        boolean printScore = o.getParameter().equals("Score");

        if (printAP) {
            networkWithCostGraphviz(s, gc);
            return;
        }

        if (printScore) {
            networkWithScoreGraphviz(s, gc);
            return;
        }

        plainNetworkGraphviz(s, gc);
    }

    @Override
    public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        makeOutput(event.getTime(), s, out, gc);
    }

    @Override
    public void parseExtraXML(Node n) throws SAXException {
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

            Visualization visualization;

            // try and get visualizationClass from the GC options
            String vClass = null;

            if (arg == null) {            // no arg - use predefined visualizationClass
                vClass = gc.getOptions().getVisualizationClassName();
            } else {                      // used passed in class name
                vClass = arg;
            }

            // if nothing, use ColouredNetworkVisualization
            if (vClass == null) {
                vClass = "usr.globalcontroller.visualization.PlainNetworkVisualization";
            }

            // instantiate the Visualization
            try {
                Class<? extends Visualization> visualizer;
                visualizer = Class.forName(vClass).asSubclass(Visualization.class);


                // find Constructor
                Constructor<? extends Visualization> cons =
                    visualizer.getDeclaredConstructor();

                // instantiate
                visualization = cons.newInstance();

                System.out.println("Instantiated " + vClass);

            } catch (ClassNotFoundException e) {
                throw new Error("Class not found for class name "+ vClass);
            } catch (ClassCastException e) {
                throw new Error("Class name "+vClass+" must be sub type of Visualization");
            } catch (NoSuchMethodException nsme) {
                throw new Error("Class name "+vClass+" has no null Constructor");
            } catch (InstantiationException ie) {
                throw new Error("Class name "+vClass+" cannot be instantiated");
            } catch (IllegalAccessException iae) {
                throw new Error("Class name "+vClass+" cannot be instantiated");
            } catch (InvocationTargetException ite) {
                throw new Error("Class name "+vClass+" cannot be instantiated");
            }

            visualization.setGlobalController(gc);
            visualization.visualize(s);
        }
    }

}