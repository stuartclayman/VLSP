package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.model.abstractnetwork.AbstractLink;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

public class ConnectNetworkEvent extends AbstractGlobalControllerEvent {
    int node1_= -1;
    int node2_= -1;

    public ConnectNetworkEvent(long t) {
        super(t, null);      // no engine
    }

    public ConnectNetworkEvent(int n1, int n2, long t) {
        super(t, null);      // no engine
        node1_= n1;
        node2_= n2;
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        AbstractLink link= null;
        if (node1_ >= 0) {
            link= gc.getAbstractNetwork().connectNetwork(time, node1_, node2_);
        } else {
            link= gc.getAbstractNetwork().connectNetwork(time);
        }
        try {
            JSONObject js = new JSONObject();
            js.put("success", (Boolean)true);
            if (link == null) {
                js.put("new_link",(Boolean)false);
                return js;
            }
            js.put("new_link",(Boolean)true);
            js.put("node1", (Integer)link.getNode1());
            js.put("node2", (Integer)link.getNode2());
            gc.scheduleLink(link, null, time);
            ConnectNetworkEvent cne;
            if (node1_ >= 0) {
                // Recheck connection
                cne= new ConnectNetworkEvent(node1_,node2_,time);
            } else {
                cne= new ConnectNetworkEvent(time);
            }

            // add a new event to my EventScheduler
            getEventScheduler().addEvent(cne);

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
            cn = "ConnectNetworkEvent "+node1_+" "+node2_+" "+time;
        } else {
            cn = "ConnectNetworkEvent "+time;
        }
        return cn;
    }

}
