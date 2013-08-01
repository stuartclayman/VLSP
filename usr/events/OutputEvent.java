package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import usr.output.*;
import us.monoid.json.*;

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