package usr.output;

import java.io.PrintStream;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.ANSI;
import usr.events.globalcontroller.APInformEvent;
import usr.events.Event;
import usr.globalcontroller.GlobalController;

/** Class to output network stuff */
public class OutputAPInformEvent implements OutputFunction {
    /** In fact this only requests output -- actual output occurs later */
    @Override
	public void makeOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
    }

    @Override
	public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        if (!(event instanceof APInformEvent)) {
            return;
        }

        int AP;
        int gid;
        try {
            gid = (Integer)result.get("router");
            AP = (Integer)result.get("AP");
        } catch (JSONException je) {
            return;
        }

        if (gid == AP) {
            s.println(gc.elapsedToString(
                          gc.getElapsedTime())
                      + ANSI.BLUE + " ROUTER "
                      + gid + " BECOME AP"
                      + ANSI.RESET_COLOUR);
        } else {
            s.println(gc.elapsedToString(
                          gc.getElapsedTime())
                      + ANSI.CYAN + " ROUTER "
                      + gid + " SET AP " + AP
                      + ANSI.RESET_COLOUR);
        }
    }

    @Override
	public void parseExtraXML(Node n) throws SAXException {
    }

}
