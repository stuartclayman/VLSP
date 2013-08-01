package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import us.monoid.json.*;

/** Class represents a global controller event*/
public class StartSimulationEvent extends AbstractEvent {
    public String toString() {
        String str;

        str = "StartSimulation: " + time_;
        return str;
    }

    public StartSimulationEvent(long time, EventEngine eng) {
        time_ = time;
        engine_ = eng;
    }

    public JSONObject execute(GlobalController gc) throws
    InstantiationException {
        gc.startSimulation(time_);
        JSONObject json = new JSONObject();
        try {
            json.put("success", true);
            json.put("msg", "Simulation started");
        } catch (JSONException je) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in StartLinkEvent should not occur");
        }

        return json;
    }

}