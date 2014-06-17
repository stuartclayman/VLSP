/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rgc.probdistributions.ProbDistribution;
import rgc.probdistributions.ProbException;
import rgc.xmlparse.ReadXMLUtils;
import rgc.xmlparse.XMLNoTagException;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.model.abstractnetwork.AbstractLink;
import usr.events.globalcontroller.EndLinkEvent;
import usr.events.globalcontroller.EndRouterEvent;
import usr.events.globalcontroller.StartRouterEvent;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/**
 * This engine uses probability distributions to add events into the
 * event library -- in this case Links have a Minimum and Maximum number of links
 * They would like
 */
public class ProbabilisticMaxLinkEventEngine extends ProbabilisticEventEngine {
    HashMap<Integer, Integer> routerMaxLinkCount_;
    HashMap<Integer, Integer> routerMinLinkCount_;
    ProbDistribution extraLinkDist_ = null;

    public ProbabilisticMaxLinkEventEngine(int time, String parms) throws EventEngineException {
        super(time);
        Document doc = parseXMLHead(parms,"ProbabilisticMaxLinkEventEngine");
        parseXMLExtra(doc);
        parseXMLMain(doc,"ProbabilisticMaxLinkEventEngine");
        routerMaxLinkCount_ = new HashMap<Integer, Integer>();
        routerMinLinkCount_ = new HashMap<Integer, Integer>();
    }

    @Override
    public void preceedEvent(Event e, EventScheduler s, EventDelegate g) {
    	try {
            if (e instanceof EndRouterEvent) {
                precedeEndRouter((EndRouterEvent)e, s, (GlobalController)g);
            } else if (e instanceof EndLinkEvent) {

                precedeEndLink((EndLinkEvent)e, s, (GlobalController)g);
            }
    	}
    	catch (Exception ex) {
            ex.printStackTrace();
    	}
    }

    /** Add or remove events following a simulation event */
    @Override
    public void followEvent(Event e, EventScheduler s, JSONObject response, EventDelegate g) {
        if (e instanceof StartRouterEvent) {
            StartRouterEvent sre = (StartRouterEvent)e;
            followRouter(sre, s, response, (GlobalController)g);
        }
    }


    private void precedeEndRouter(EndRouterEvent e, EventScheduler s, GlobalController g) {
        long now = e.getTime();

        try {
            int router = e.getNumber(g);
            List<Integer> outLinks = g.getOutLinks(router);

            for (int o : outLinks) {
                checkRouter(o, s, g, now);
            }
        } catch (InstantiationException ie) {
            Logger.getLogger("log").logln(
                                          USR.ERROR, leadin()
                                          + "Error getting address " + e);
        }

    }

    private int followRouter(StartRouterEvent e, EventScheduler s, JSONObject response, GlobalController g) {
        long now = e.getTime();
        int routerId = scheduleDeath(now, s, response, g, this);
        int nlinks = 0;
        int created = 0;

        if (routerId >= 0) {
            nlinks = howManyLinks();
            initMLRouter(e, s, response, g, nlinks);
            created = createNLinks(s, routerId, g, now, nlinks);
        }

        scheduleNextRouter(now, s, g, this);
        return created;
    }

    private void precedeEndLink(EndLinkEvent e, EventScheduler s, GlobalController g) {
        long now = e.getTime();

        try {
            int router = e.getRouter1(g);
            checkRouter(router, s, g, now);
            router = e.getRouter2(g);
            checkRouter(router, s, g, now);
        } catch (InstantiationException ie) {
            Logger.getLogger("log").logln(
                                          USR.ERROR, leadin()
                                          + "Error getting address " + e);
        }
    }

    private void checkRouter(int router, EventScheduler s, GlobalController g, long now) {
        List<Integer> outLinks = g.getOutLinks(router);
        int minLinks = routerMinLinkCount_.get(router);

        if (outLinks.size() <= minLinks) {
            createNLinks(s, router, g, now, outLinks.size() + 1 - minLinks);
        }
    }

    /** Choose n links -- schedule their creation
     * -- return number created*/
    private int createNLinks(EventScheduler s, int routerId, GlobalController g, long now, int noLinks) {
        // Get a list of nodes which can be connected to
        ArrayList<Integer> nodes = new ArrayList<Integer>();

        for (int i : g.getRouterList()) {
            if (g.getAbstractNetwork().getAllOutLinks(i).length < routerMaxLinkCount_.get(i)) {
                nodes.add(i);
            }
        }
        // remove nodes already connected
        int idx= nodes.indexOf(routerId);
        if (idx != -1)
            nodes.remove(idx);
        int [] outlinks = g.getAbstractNetwork().getAllOutLinks(routerId);

        for (Integer l : outlinks) {
            idx= nodes.indexOf(l);
            //System.err.println("Looking at link from "+routerId+" "+l);
            if (idx != -1) {
                nodes.remove(idx);
                //System.err.println("Removing link from "+routerId+" "+l);
            }
        }

        // Pink links and schedule
        List<Integer> picked = linkPicker_.pickNLinks(nodes, g, noLinks, routerId);

        for (int i : picked) {
            g.scheduleLink(new AbstractLink(routerId,i),this,now);
        }

        return picked.size();
    }

    private void initMLRouter(StartRouterEvent e, EventScheduler s, JSONObject response, GlobalController g, int nlinks) {
        try {
            int routerId = (Integer)response.get("routerID");
            routerMinLinkCount_.put(routerId, nlinks);
            int extraLinks = extraLinkDist_.getIntVariate();
            routerMaxLinkCount_.put(routerId, nlinks + extraLinks);
        } catch (JSONException ex) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Unexpected JSON error in initMLRouter"+ex.getMessage());
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "JSON "+response);

            return;
        } catch (ProbException ex) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Unexpected ProbException in initMLRouter");
        }
    }

    private void parseXMLExtra(Document doc) throws EventEngineException {
        try {
            NodeList eld = doc.getElementsByTagName("ExtraLinkDist");
            extraLinkDist_ = ProbDistribution.parseProbDist(eld,
                                                            "ExtraLinkDist");

            if (extraLinkDist_ == null) {
                throw new EventEngineException("Must specify ExtraLinkDist "
                                               + "in ProbablisiticMaxLinkEventEngine");
            }

            ReadXMLUtils.removeNode(eld.item(0).getParentNode(),
                                    "ExtraLinkDist", "ProbabilisticMaxLinkEventEngine");
        } catch (ProbException e) {
            throw new EventEngineException("Must specify ExtraLinkDist "
                                           + "correctly in ProbablisiticMaxLinkEventEngine "
                                           + e.getMessage());
        } catch (SAXException e) {
            throw new EventEngineException("Must specify ExtraLinkDist "
                                           + "in ProbablisiticMaxLinkEventEngine");
        } catch (XMLNoTagException e) {
            throw new EventEngineException("Must specify ExtraLinkDist "
                                           + "in ProbablisiticMaxLinkEventEngine");
        }
    }

    private String leadin() {
        return "ProbMLEventEngine: ";
    }

}
