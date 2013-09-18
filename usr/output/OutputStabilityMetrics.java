package usr.output;

import java.io.PrintStream;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.globalcontroller.GlobalController;

/** Class to output network stuff */
public class OutputStabilityMetrics implements OutputFunction {
    private boolean first_ = true;

    /** In fact this only requests output -- actual output occurs later */
    @Override
	public void makeOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
        if (first_) {
            p.println("Time Nodes Links d_bar d_max");
            first_ = false;
        }

        p.println(gc.getNoRouters() + " " + gc.getNoLinks() + " "
                  + gc.getAbstractNetwork().getdbar() + " "
                  + gc.getAbstractNetwork().getdmax());
    }

    @Override
	public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        makeOutput(event.getTime(), s, out, gc);
    }

    @Override
	public void parseExtraXML(Node n) throws SAXException {
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        return "OSM: ";
    }

}