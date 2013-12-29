package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class NetStatsEvent extends AbstractGlobalControllerEvent {
    String stats_;

    public NetStatsEvent(long time, String stats) {
        super(time, null);
        stats_ = stats;
    }

    @Override
	public String toString() {
        String str;

        str = "NetStats: " + time;
        return str;
    }

    @Override
	public JSONObject execute(GlobalController gc) {
        JSONObject json = new JSONObject();

        try {
            json.put("success", (Boolean)true);
            json.put("msg", "Stats " + stats_);
            json.put("netstats", stats_);
        } catch (JSONException js) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in EndRouterEvent should not occur");
        }

        return json;
    }

}
