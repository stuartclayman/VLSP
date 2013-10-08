package usr.events;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.abstractnetwork.AbstractLink;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

public class CheckIsolatedEvent extends AbstractEvent {

    int node_;

    /**
     * Event checks if node is isolated and adds links if it would be
     * @param node -- number of node to check
     * @param time -- time to make check
     */
    public CheckIsolatedEvent(int node, long time)
    {
        node_= node;
        time_= time;
    }

    @Override
    public JSONObject execute(GlobalController gc)
            throws InstantiationException {
        AbstractLink link= null;
        link= gc.getAbstractNetwork().checkIsolated(time_, node_ , gc);
        try {
            JSONObject js = new JSONObject();
            js.put("success", (Boolean)false);
            if (link == null) {
                js.put("new_link",(Boolean)false);
                return js;
            }
            js.put("new_link",(Boolean)true);
            js.put("node1", (Integer)link.getNode1());
            js.put("node2", (Integer)link.getNode2());
            gc.scheduleLink(link, null, time_);
            return js;
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in ConnectNetworkEvent should not occur");
        }
        return null;
    }

    @Override
    public String toString() {
        String cn = "CheckIsolatedEvent "+node_+" "+time_;

        return cn;
    }

}
