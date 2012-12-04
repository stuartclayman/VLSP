/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import usr.globalcontroller.*;
import rgc.xmlparse.*;
import rgc.probdistributions.*;
import usr.logging.*;
import usr.common.Pair;
import usr.APcontroller.*;
import usr.events.*;
import us.monoid.json.*;
import org.w3c.dom.Document;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.io.*;
import java.util.*;
import java.lang.*;

/**
 * This engine uses probability distribtions to add events into the
 * event library
 */
public class ProbabilisticMaxLinkEventEngine extends ProbabilisticEventEngine
{
    HashMap <Integer,Integer> routerMaxLinkCount_;
    HashMap <Integer,Integer> routerMinLinkCount_;
    ProbDistribution extraLinkDist_= null;
    
    public ProbabilisticMaxLinkEventEngine(int time, String parms) 
        throws EventEngineException 
    {
        super(time);
        Document doc= parseXMLHead(parms);
        super.parseXMLDoc(doc,parms);
        routerMaxLinkCount_= new HashMap <Integer,Integer>();
        routerMinLinkCount_= new HashMap <Integer,Integer>();
    }
    
public void precedeEvent(Event e, EventScheduler s, 
        GlobalController g)
{
    if (e instanceof EndRouterEvent) {
        precedeEndRouter((EndRouterEvent) e, s, g);
    } else if (e instanceof EndLinkEvent) {
        precedeEndLink((EndLinkEvent) e,s,g);
    }
}   

private void precedeEndRouter(EndRouterEvent e, EventScheduler s,
    GlobalController g)
{
    long now= e.getTime();
    try {
        int router= e.getNumber(g);
        int []outLinks= g.getOutLinks(router);
        for (int o: outLinks) {
            checkRouter(o, s, g, now);
        }
    } catch (InstantiationException ie) {
        Logger.getLogger("log").logln(
            USR.ERROR, leadin()+
            "Error getting address "+e);
    }
}

private void precedeEndLink(EndLinkEvent e, EventScheduler s,
    GlobalController g)
{
    long now= e.getTime();
    try {
        int router= e.getRouter1(g);
        checkRouter(router,s,g, now);
        router= e.getRouter2(g);
        checkRouter(router,s,g,now);
    } catch (InstantiationException ie) {
         Logger.getLogger("log").logln(
            USR.ERROR, leadin()+
            "Error getting address "+e);
    }
}

private void checkRouter(int router, EventScheduler s, 
    GlobalController g, long now)
{
    int []outLinks= g.getOutLinks(router);
    int minLinks= routerMinLinkCount_.get(router);
    if (outLinks.length <= minLinks) {
        createNLinks(s, router, g, now, outLinks.length +1 - minLinks);
    }
}

private int createNLinks(EventScheduler s, int routerId,
    GlobalController g, long now, int noLinks)   
{
    // Get a list of nodes which can be connected to
    ArrayList <Integer>nodes = new ArrayList<Integer>();
    for (int i: g.getRouterList()) {
        if (g.getOutLinks(i).length < routerMaxLinkCount_.get(i)) {
            nodes.add(i);
        }
    }
        nodes.remove(nodes.indexOf(routerId));
    int [] outlinks = g.getOutLinks(routerId);
    for (Integer l : outlinks)
        nodes.remove(nodes.indexOf(l));
    
}
    
/** Add or remove events following a simulation event */
public void followEvent(Event e, EventScheduler s,
    JSONObject response, GlobalController g)                          
{
    if (e instanceof StartRouterEvent) {
        int nlinks= 
            super.followRouter((StartRouterEvent)e, s, response, g);
        if (nlinks >= 0) 
            initMLRouter((StartRouterEvent)e, s, response, g, nlinks);
    } 
}

private void initMLRouter(StartRouterEvent e, EventScheduler s, 
    JSONObject response, GlobalController g, int nlinks)
{
    try {
        int routerId=(Integer)response.get("routerID");
        routerMinLinkCount_.put(routerId,nlinks);
        int extraLinks= extraLinkDist_.getIntVariate();
        routerMaxLinkCount_.put(routerId,nlinks+extraLinks);
    } catch (JSONException ex) {
        Logger.getLogger("log").logln(USR.ERROR,
            leadin() + "Unexpected JSON error in initMLRouter");
        return ;
    } catch (ProbException ex) {
        Logger.getLogger("log").logln(USR.ERROR,
            leadin() + "Unexpected ProbException in initMLRouter");
    }
}

/** Parse the head of the XML file to get info specific to this
 * return document in form for ProbabilisticEventEngine*/
private Document parseXMLHead(String fName) throws EventEngineException {
    Document doc;
    try {
        DocumentBuilderFactory docBuilderFactory =
            DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder =
            docBuilderFactory.newDocumentBuilder();
        doc = docBuilder.parse(new File(fName));

        // normalize text representation
        doc.getDocumentElement().normalize();
        String basenode = doc.getDocumentElement().getNodeName();
        if (!basenode.equals("ProbabilisticEngine")) throw new
                  SAXException(
                "Base tag should be ProbabilisticEngine");
    } catch (java.io.FileNotFoundException e) { 
        throw new EventEngineException(
        "Parsing ProbabilisticEventEngine: Cannot find file "+ fName);
    } catch (SAXParseException err) { 
        throw new EventEngineException(
            "Parsing ProbabilisticEventEngine: error" + ", line "+ 
            err.getLineNumber() + ", uri " + err.getSystemId());
    }catch (SAXException e) { 
        throw new EventEngineException(
        "Parsing ProbabilisticEventEngine: Exception in SAX XML parser"
        + e.getMessage());
    }catch (Throwable t) { 
        throw new EventEngineException(
           "Parsing ProbabilisticEventEngine: " + t.getMessage());
    }    
    try 
    {
        NodeList eld = doc.getElementsByTagName("ExtraLinkDist");
        extraLinkDist_ = ProbDistribution.parseProbDist(eld,
            "ExtraLinkDist");
        if (extraLinkDist_ == null) {
            throw new EventEngineException("Must specify ExtraLinkDist "+
                "in ProbablisiticMaxLinkEventEngine");
        }
        
    } catch (ProbException e) {
        throw new EventEngineException("Must specify ExtraLinkDist "+
                "correctly in ProbablisiticMaxLinkEventEngine "+
                e.getMessage());
    } catch (SAXException e) {
        throw new EventEngineException("Must specify ExtraLinkDist "+
                "in ProbablisiticMaxLinkEventEngine");
    }catch (XMLNoTagException e) {
        throw new EventEngineException("Must specify ExtraLinkDist "+
                "in ProbablisiticMaxLinkEventEngine");
    }
    return doc;
}

private String leadin(){
        return "ProbMLEventEngine: ";
}
    
}
