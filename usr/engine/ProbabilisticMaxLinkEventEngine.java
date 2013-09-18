/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rgc.probdistributions.ProbDistribution;
import rgc.probdistributions.ProbException;
import rgc.xmlparse.ReadXMLUtils;
import rgc.xmlparse.XMLNoTagException;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.events.EndLinkEvent;
import usr.events.EndRouterEvent;
import usr.events.Event;
import usr.events.EventScheduler;
import usr.events.StartLinkEvent;
import usr.events.StartRouterEvent;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/**
 * This engine uses probability distribtions to add events into the
 * event library
 */
public class ProbabilisticMaxLinkEventEngine extends
ProbabilisticEventEngine {
    HashMap<Integer, Integer> routerMaxLinkCount_;
    HashMap<Integer, Integer> routerMinLinkCount_;
    ProbDistribution extraLinkDist_ = null;
    HashMap<Integer, ArrayList<Integer> > pendingLinks_;

    public ProbabilisticMaxLinkEventEngine(int time, String parms)
    throws EventEngineException {
        super(time);
        Document doc = parseXMLHead(parms);
        parseXMLExtra(doc);
        parseXMLMain(doc);
        routerMaxLinkCount_ = new HashMap<Integer, Integer>();
        routerMinLinkCount_ = new HashMap<Integer, Integer>();
        pendingLinks_ = new HashMap<Integer, ArrayList<Integer> >();
    }

    public void precedeEvent(Event e, EventScheduler s, GlobalController g) {
        if (e instanceof EndRouterEvent) {
            precedeEndRouter((EndRouterEvent)e, s, g);
        } else if (e instanceof EndLinkEvent) {
            precedeEndLink((EndLinkEvent)e, s, g);
        }
    }

    /** Add or remove events following a simulation event */
    @Override
	public void followEvent(Event e, EventScheduler s, JSONObject response, GlobalController g) {
        if (e instanceof StartRouterEvent) {
            StartRouterEvent sre = (StartRouterEvent)e;
            followRouter(sre, s, response, g);
        } else if (e instanceof StartLinkEvent) {
            followEndLinkEvent((StartLinkEvent)e, g);
        }
    }

    private void followEndLinkEvent(StartLinkEvent e, GlobalController g) {
        try {
            int r1 = e.getRouter1(g);
            int r2 = e.getRouter2(g);
            unscheduleLink(r1, r2);
        } catch (InstantiationException ex) {
            Logger.getLogger("log").logln(
                USR.ERROR, leadin()
                + "Error getting address in followEndLinkEvent " + e);
        }
    }

    private void precedeEndRouter(EndRouterEvent e, EventScheduler s, GlobalController g) {
        long now = e.getTime();

        try {
            int router = e.getNumber(g);
            int [] outLinks = g.getOutLinks(router);

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
        int [] outLinks = g.getOutLinks(router);
        int minLinks = routerMinLinkCount_.get(router);

        if (outLinks.length <= minLinks) {
            createNLinks(s, router, g, now, outLinks.length + 1 - minLinks);
        }
    }

    /** Choose n links -- schedule their creation
     * -- return number created*/
    private int createNLinks(EventScheduler s, int routerId, GlobalController g, long now, int noLinks) {
        // Get a list of nodes which can be connected to
        ArrayList<Integer> nodes = new ArrayList<Integer>();

        for (int i : g.getRouterList()) {
            if (g.getOutLinks(i).length < routerMaxLinkCount_.get(i)) {
                nodes.add(i);
            }
        }

        nodes.remove(nodes.indexOf(routerId));
        int [] outlinks = g.getOutLinks(routerId);

        for (Integer l : outlinks) {
            nodes.remove(nodes.indexOf(l));
        }

        ArrayList<Integer> tmp = pendingLinks_.get(routerId);

        if (tmp != null) {
            for (int i : tmp) {
                int t = nodes.indexOf(i);

                if (t >= 0) {
                    nodes.remove(t);
                }
            }
        }

        ArrayList<Integer> picked = linkPicker_.pickNLinks(nodes, g,
                                                           noLinks, routerId);

        for (int i : picked) {
            StartLinkEvent e = new StartLinkEvent(now, this, i,
                                                  routerId);
            s.addEvent(e);
            scheduleLink(routerId, i);
        }

        return picked.size();
    }

    private void scheduleLink(int l1, int l2) {
        ArrayList<Integer> tmp = pendingLinks_.get(l1);

        if (tmp == null) {
            tmp = new ArrayList<Integer>(1);
            pendingLinks_.put(l1, tmp);
        }

        tmp.add(l2);
        tmp = pendingLinks_.get(l2);

        if (tmp == null) {
            tmp = new ArrayList<Integer>(1);
            pendingLinks_.put(l2, tmp);
        }

        tmp.add(l1);
    }

    private void unscheduleLink(int l1, int l2) {
        ArrayList<Integer> tmp = pendingLinks_.get(l1);
        tmp.remove(tmp.indexOf(l2));
        tmp = pendingLinks_.get(l2);
        tmp.remove(tmp.indexOf(l1));
    }

    private void initMLRouter(StartRouterEvent e, EventScheduler s, JSONObject response, GlobalController g, int nlinks) {
        try {
            int routerId = (Integer)response.get("routerID");
            routerMinLinkCount_.put(routerId, nlinks);
            int extraLinks = extraLinkDist_.getIntVariate();
            routerMaxLinkCount_.put(routerId, nlinks + extraLinks);
        } catch (JSONException ex) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Unexpected JSON error in initMLRouter");
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
                                    "ExtraLinkDist", "ProbabilisticEngine");
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