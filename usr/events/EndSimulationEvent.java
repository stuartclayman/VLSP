package usr.events;

import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndSimulationEvent extends AbstractEvent {
    public EndSimulationEvent(long time) {
        time_ = time;
    }

    @Override
    public String toString() {
        return new String("EndSimulation " + time_);
    }

    @Override
    public JSONObject execute(EventDelegate ed) throws InstantiationException {
        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("msg", "Shut Down Sent to Controller");
            jsobj.put("success", true);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in EndSimulationEvent should not occur");
        }

        ed.onEventSchedulerStop(getTime());
        return jsobj;
    }

}
