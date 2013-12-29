package usr.events.globalcontroller;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class APInformEvent extends AbstractGlobalControllerEvent {
    int routerNo_;
    int AP_;

    public APInformEvent(long time, int rid, int AP) {
        super(time, null);      // no engine
        AP_ = AP;
        routerNo_ = rid;
    }

    @Override
    public String toString() {
        String str;

        str = "APInform: " + time + " router " + routerNo_ + " AP " + AP_;
        return str;
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject json = new JSONObject();

        try {
            json.put("success", (Boolean)true);
            json.put("msg", "Router " + routerNo_ + " attached to AP " +
                     AP_);
            json.put("router", (Integer)routerNo_);
            json.put("AP", (Integer)AP_);
        } catch (JSONException js) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in APInformEvent should not occur");
        }

        return json;
    }

}
