package usr.output;

import usr.logging.*;
import java.util.*;
import java.io.*;
import usr.globalcontroller.GlobalController;
import usr.events.*;
import us.monoid.json.*;
import usr.common.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Class to output network stuff */
public class OutputAPInformEvent implements OutputFunction {
    /** In fact this only requests output -- actual output occurs later */
    public void makeOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
    }

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

    public void parseExtraXML(Node n) throws SAXException {
    }

}