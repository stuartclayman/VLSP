package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.APcontroller.APController;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndWarmupRouterEvent extends AbstractGlobalControllerEvent {
    long starttime_ = 0;
    EventEngine engine_ = null;

    public EndWarmupRouterEvent(long starttime, long time, EventEngine eng) {
        starttime_ = starttime;
        time_ = time;
        engine_ = eng;
    }

    @Override
    public String toString() {
        String str;

        str = "EndWarmupRouter: lasted from " + starttime_ + " to " + time_;
        return str;
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject json = new JSONObject();
        APController ap = gc.getAPController();

        ap.removeWarmUpNode(starttime_, time_);
        try {
            json.put("success", (Boolean)true);
        } catch (JSONException js) {
            Logger.getLogger("log").logln(
                                          USR.ERROR,
                                          "JSONException in EndRouterEvent should not occur");
        }

        return json;
    }

    @SuppressWarnings("unused")
    private String leadin() {
        return "EWRE: ";
    }

}
