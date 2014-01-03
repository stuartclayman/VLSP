package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class ListRoutersEvent extends AbstractGlobalControllerEvent {
    public ListRoutersEvent(long time, EventEngine eng) {
        super(time, eng);
    }

    @Override
	public String toString() {
        String str;

        str = "ListRouters: " + time;
        return str;
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();

        try {
            jsobj.put("success", true);

            for (BasicRouterInfo info : gc.getAllRouterInfo()) {
                array.put(info.getId());
            }

            jsobj.put("type", "router");
            jsobj.put("list", array);
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in StartLinkEvent should not occur");
        }

        return jsobj;
    }

}
