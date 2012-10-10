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
public class OutputNetStatsEvent implements OutputFunction
{
/** In fact this only requests output -- actual output occurs later */
public void makeOutput(long t, PrintStream p, OutputType o,
    GlobalController gc)
{
    
}

public void makeEventOutput(Event event, JSONObject result, 
    PrintStream s, OutputType out, GlobalController gc)
{
    if (!(event instanceof NetStatsEvent)) 
        return;
    String netstats;
    try {
        netstats= (String)result.get("netstats");
    } catch (JSONException je) {
        return;
    }
    s.println(netstats);
}


public void parseExtraXML(Node n) throws SAXException
{
}

}
