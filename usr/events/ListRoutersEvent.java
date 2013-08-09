package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import us.monoid.json.*;
import usr.common.*;

/** Class represents a global controller event*/
public class ListRoutersEvent extends AbstractEvent {
    public ListRoutersEvent(long time, EventEngine eng) {
        time_ = time;
        engine_ = eng;
    }

    public String toString() {
        String str;

        str = "ListRouters: " + time_;
        return str;
    }

    public JSONObject execute(GlobalController gc) throws InstantiationException {
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
