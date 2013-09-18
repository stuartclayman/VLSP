package usr.events;

import us.monoid.json.JSONObject;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;
import usr.output.OutputType;

/** Class represents a global controller event*/
public class OutputEvent extends AbstractEvent {
    private OutputType output_;

    public OutputEvent(long time, EventEngine eng, OutputType ot) {
        time_ = time;
        engine_ = eng;
        output_ = ot;
    }

    public String toString() {
        return new String("OutputEvent " + time_ + " " + output_);
    }

    public JSONObject execute(GlobalController gc)
    throws InstantiationException {
        JSONObject jsobj = new JSONObject();

        gc.produceOutput(time_, output_);
        try {
            jsobj.put("success", (Boolean)true);
            jsobj.put("msg", "Output produced");
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in OutputEvent should not occur");
        }

        return jsobj;
    }

}