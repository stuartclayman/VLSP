package usr.output;

import usr.logging.*;
import java.util.*;
import java.io.PrintStream;
import usr.globalcontroller.GlobalController;
import usr.globalcontroller.GlobalController;
import usr.events.*;
import us.monoid.json.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.*;

/** Class to output network stuff */
public class OutputStabilityMetrics implements OutputFunction
{

private boolean first_= true;    
    
/** In fact this only requests output -- actual output occurs later */
public void makeOutput(long t, PrintStream p, OutputType o,
    GlobalController gc)
{
    if (first_) {
        p.println("Time Nodes Links d_bar d_max");
        first_= false;
    }
    p.println(gc.getNoRouters()+" "+gc.getNoLinks()+" "+
        gc.getAbstractNetwork().getdbar()+" "+
        gc.getAbstractNetwork().getdmax());
}

public void makeEventOutput(Event event, JSONObject result, 
    PrintStream s, OutputType out, GlobalController gc)
{
    makeOutput(event.getTime(),s,out,gc);
}

public void parseExtraXML(Node n) throws SAXException
{
}

/**
 * Create the String to print out before a message
 */
String leadin() {
    return "OSM: ";
}
}
