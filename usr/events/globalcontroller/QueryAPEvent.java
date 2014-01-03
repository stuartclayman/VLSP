package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONObject;
import usr.APcontroller.APController;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class QueryAPEvent extends AbstractGlobalControllerEvent {
    APController apc_;

    public QueryAPEvent(long time, EventEngine eng, APController ap) {
        super(time, eng);
        apc_ = ap;
    }

    @Override
    public String toString() {
        return new String("QueryAPEvent " + time);
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        apc_.controllerUpdate(time, gc);
        JSONObject jsobj = new JSONObject();
        try {
            jsobj.put("success", (Boolean)true);
            jsobj.put("msg", "APEvent Queried");
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                                          USR.ERROR,
                                          "JSONException in QueryAPEvent should not occur");
        }

        return jsobj;
    }

    /** Perform logic which follows an event */
    @Override
    public void followEvent(JSONObject response, GlobalController g) {
        long newTime = time + g.getAPControllerConsiderTime();
        QueryAPEvent e = new QueryAPEvent(newTime, engine, apc_);
        getEventScheduler().addEvent(e);
    }

}
