package usr.output;

import java.io.PrintStream;
import usr.globalcontroller.GlobalController;
import usr.APcontroller.APController;
import usr.events.Event;
import us.monoid.json.*;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;

/** Class to output summary stats  */
class OutputSummary implements OutputFunction {

    public void makeOutput(long time, PrintStream s, OutputType o, GlobalController gc) {

        if (o.isFirst()) {

            s.println("#No_nodes no_links no_aps tot_ap_dist mean_life mean_AP_life");
            o.setFirst(false);
        }
        APController apc = gc.getAPController();
        s.print(gc.getNoRouters()+" "+gc.getLinkCount()+" "+
                apc.getNoAPs());
        apc.controllerUpdate(time, gc);
        s.print(" "+apc.APTrafficEstimate(gc));
        s.print(" "+apc.meanNodeLife()+" "+apc.meanAPLife());
        s.print(" "+apc.meanAPLifeSoFar(time));
        s.println();
    }

    public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        makeOutput(event.getTime(), s, out, gc);
    }

    public void parseExtraXML(Node n) throws SAXException {
    }


}
