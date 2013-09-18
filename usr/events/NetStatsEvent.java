package usr.events;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class NetStatsEvent extends AbstractEvent {
    String stats_;

    public NetStatsEvent(long time, String stats) {
        time_ = time;
        stats_ = stats;
    }

    public String toString() {
        String str;

        str = "NetStats: " + time_ + " stats " + stats_;
        return str;
    }

    public JSONObject execute(GlobalController gc) {
        JSONObject json = new JSONObject();

        try {
            json.put("success", (Boolean)true);
            json.put("msg", "Stats " + stats_);
            json.put("netstats", (String)stats_);
        } catch (JSONException js) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in EndRouterEvent should not occur");
        }

        return json;
    }

}