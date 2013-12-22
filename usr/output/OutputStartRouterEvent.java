package usr.output;

import java.io.PrintStream;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.ANSI;
import usr.events.Event;
import usr.events.globalcontroller.StartRouterEvent;
import usr.globalcontroller.GlobalController;

/** Class to output network stuff */
public class OutputStartRouterEvent implements OutputFunction {
    /** In fact this only requests output -- actual output occurs later */
    @Override
	public void makeOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
    }

    @Override
	public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        if (!(event instanceof StartRouterEvent)) {
            return;
        }

        int rId;
        try {
            rId = (Integer)result.get("routerID");
        } catch (JSONException je) {
            return;
        }

        s.println(gc.elapsedToString(gc.getElapsedTime())
                  + ANSI.GREEN + " START ROUTER " + rId + ANSI.RESET_COLOUR);
    }

    @Override
	public void parseExtraXML(Node n) throws SAXException {
    }

}
