package usr.events;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.abstractnetwork.AbstractLink;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

public class ConnectNetworkEvent extends AbstractEvent {
    int node1_= -1;
    int node2_= -1;

    public ConnectNetworkEvent(long t)
    {
        time_= t;
    }

    public ConnectNetworkEvent(int n1, int n2, long t)
    {
        time_= t;
        node1_= n1;
        node2_= n2;
    }

    @Override
    public JSONObject execute(GlobalController gc)
            throws InstantiationException {
        AbstractLink link= null;
        if (node1_ >= 0) {
            link= gc.getAbstractNetwork().connectNetwork(time_, node1_, node2_, gc);
        } else {
            link= gc.getAbstractNetwork().connectNetwork(time_, gc);
        }
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
            ConnectNetworkEvent cne;
            if (node1_ >= 0) {
                // Recheck connection
                cne= new ConnectNetworkEvent(node1_,node2_,time_);
            } else {
                cne= new ConnectNetworkEvent(time_);
            }
            gc.addEvent(cne);
            return js;
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in ConnectNetworkEvent should not occur");
        }
        return null;
    }

    @Override
    public String toString() {
        String cn;
        if (node1_ >= 0) {
            cn = "ConnectNetworkEvent "+node1_+" "+node2_+" "+time_;
        } else {
            cn = "ConnectNetworkEvent "+time_;
        }
        return cn;
    }

}
