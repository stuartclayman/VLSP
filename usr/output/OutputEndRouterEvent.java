package usr.output;

import java.io.PrintStream;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.ANSI;
import usr.events.globalcontroller.EndRouterEvent;
import usr.events.Event;
import usr.globalcontroller.GlobalController;

/** Class to output network stuff */
public class OutputEndRouterEvent implements OutputFunction {
    /** In fact this only requests output -- actual output occurs later */
    @Override
	public void makeOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
    }

    @Override
	public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        //System.err.println("STOP EVENT "+ event.getClass().toString());
        if (!(event instanceof EndRouterEvent)) {
            return;
        }

        int rId;
        try {
            rId = (Integer)result.get("router");
        } catch (JSONException je) {
            return;
        }

        s.println(gc.elapsedToString(gc.getElapsedTime())
                  + ANSI.RED + " STOP ROUTER " + rId + ANSI.RESET_COLOUR);
    }

    @Override
	public void parseExtraXML(Node n) throws SAXException {
    }

}
