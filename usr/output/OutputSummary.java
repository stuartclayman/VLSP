package usr.output;

import java.io.PrintStream;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONObject;
import usr.APcontroller.APController;
import usr.events.Event;
import usr.globalcontroller.GlobalController;

/** Class to output summary stats  */
public class OutputSummary implements OutputFunction {

    @Override
    public void makeOutput(long time, PrintStream s, OutputType o, GlobalController gc) {

        if (o.isFirst()) {

            s.println("#No_nodes no_links no_aps tot_ap_dist mean_life mean_AP_life");
            o.setFirst(false);
        }
        APController apc = gc.getAPController();
        s.print(gc.getNoRouters()+" "+gc.getNoLinks()+" "+
                apc.getNoAPs());
        apc.controllerUpdate(time, gc);
        s.print(" "+apc.APTrafficEstimate(gc));
        s.print(" "+apc.meanNodeLife()+" "+apc.meanAPLife());
        s.print(" "+apc.meanAPLifeSoFar(time));
        s.println();
    }

    @Override
    public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        makeOutput(event.getTime(), s, out, gc);
    }

    @Override
    public void parseExtraXML(Node n) throws SAXException {
    }

}