package usr.output;

import java.io.PrintStream;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.globalcontroller.GlobalController;

/** This interface is for any function producing output */
public interface OutputFunction {
    public void makeOutput(long time, PrintStream s, OutputType out, GlobalController gc);
    public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc);
    public void parseExtraXML(Node n) throws SAXException;
}
