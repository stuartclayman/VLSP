package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import us.monoid.json.*;

/** Class represents a global controller event*/
public class EndSimulationEvent extends AbstractEvent {
    public EndSimulationEvent(long time, EventEngine eng) {
        time_ = time;
        engine_ = eng;
    }

    public String toString() {
        return new String("EndSimulation " + time_);
    }

    public JSONObject execute(GlobalController gc) throws
    InstantiationException {
        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("msg", "Shut Down Sent to Controller");
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in EndSimulationEvent should not occur");
        }

        gc.deactivate();
        return jsobj;
    }

}