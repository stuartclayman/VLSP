package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONObject;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;
import usr.output.OutputType;

/** Class represents a global controller event*/
public class OutputEvent extends AbstractGlobalControllerEvent {
    private OutputType output_;

    public OutputEvent(long time, EventEngine eng, OutputType ot) {
        super(time, eng);
        output_ = ot;
    }

    @Override
    public String toString() {
        return new String("OutputEvent " + time + " " + output_);
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject jsobj = new JSONObject();

        gc.produceOutput(time, output_);

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
