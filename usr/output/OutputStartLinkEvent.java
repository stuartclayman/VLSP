package usr.output;

import usr.logging.*;
import java.util.*;
import java.io.*;
import usr.globalcontroller.GlobalController;
import usr.globalcontroller.GlobalController;
import usr.events.*;
import us.monoid.json.*;
import usr.common.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Class to output network stuff */
public class OutputStartLinkEvent implements OutputFunction
{
/** In fact this only requests output -- actual output occurs later */
public void makeOutput(long t, PrintStream p, OutputType o,
    GlobalController gc)
{
    
}

public void makeEventOutput(Event event, JSONObject result, 
    PrintStream s, OutputType out, GlobalController gc)
{
    if (!(event instanceof StartLinkEvent)) 
        return;
    int rId1;
    int rId2;
    try {
        rId1= (Integer)result.get("router1");
        rId2= (Integer)result.get("router2");
    } catch (JSONException je) {
        return;
    }
    s.println(gc.elapsedToString(gc.getElapsedTime())
                + ANSI.BLUE + " CREATE LINK " + rId1 +
                " TO " + rId2 + ANSI.RESET_COLOUR);
}


public void parseExtraXML(Node n) throws SAXException
{
}

}
