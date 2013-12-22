package usr.output;

import java.io.PrintStream;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.events.globalcontroller.NetStatsEvent;
import usr.globalcontroller.GlobalController;

/** Class to output network stuff */
public class OutputNetStatsEvent implements OutputFunction {
    /** In fact this only requests output -- actual output occurs later */
    @Override
	public void makeOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
    }

    @Override
	public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        if (!(event instanceof NetStatsEvent)) {
            return;
        }

        String netstats;
        try {
            netstats = (String)result.get("netstats");
        } catch (JSONException je) {
            return;
        }

        s.println(netstats);
    }

    @Override
	public void parseExtraXML(Node n) throws SAXException {
    }

}
