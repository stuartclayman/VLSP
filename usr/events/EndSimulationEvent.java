package usr.events;

import us.monoid.json.JSONObject;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndSimulationEvent extends AbstractEvent {
    public EndSimulationEvent(long time, EventEngine eng) {
        time_ = time;
        engine_ = eng;
    }

    @Override
    public String toString() {
        return new String("EndSimulation " + time_);
    }

    @Override
    public JSONObject execute(GlobalController gc) throws InstantiationException {
        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("msg", "Shut Down Sent to Controller");
            jsobj.put("success", true);
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                                          USR.ERROR,
                                          "JSONException in EndSimulationEvent should not occur");
        }

        gc.deactivate();
        //gc.shutDown();
        return jsobj;
    }

}
