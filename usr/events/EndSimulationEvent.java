package usr.events;

import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndSimulationEvent extends AbstractExecutableEvent {
    public EndSimulationEvent(long time) {
        super(time, null); // no engine
    }

    @Override
    public String toString() {
        return new String("EndSimulation " + time);
    }

    @Override
    public JSONObject eventBody(EventDelegate ed) {
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
