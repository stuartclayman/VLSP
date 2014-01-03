package usr.events;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartSimulationEvent extends AbstractExecutableEvent {
    @Override
    public String toString() {
        String str;

        str = "StartSimulation: " + time;
        return str;
    }

    public StartSimulationEvent(long time) {
        super(time, null);  // no engine
    }

    @Override
    public JSONObject eventBody(EventDelegate ed) {
        ed.onEventSchedulerStart(time);
        JSONObject json = new JSONObject();
        try {
            json.put("success", true);
            json.put("msg", "Simulation started");
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in StartLinkEvent should not occur");
        }

        return json;
    }

}
