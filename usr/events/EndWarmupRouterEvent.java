package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.localcontroller.*;
import usr.engine.*;
import usr.common.*;
import us.monoid.json.*;
import usr.interactor.*;
import usr.APcontroller.*;

/** Class represents a global controller event*/
public class EndWarmupRouterEvent extends Event
{
    
long time_= 0;
long starttime_= 0;
EventEngine engine_= null;

public EndWarmupRouterEvent (long starttime, long time, 
        EventEngine eng) 
{
    time_ = time;
    engine_ = eng;
}

public String toString(){
    String str;

    str = "EndWarmupRouter: lasted from "+starttime_+" to "+ time_;
    return str;
}

public JSONObject execute(GlobalController gc) {
    
    JSONObject json = new JSONObject();
    APController ap = gc.getAPController();
    ap.removeWarmUpNode(starttime_, time_);
    try {
        json.put("success", (Boolean)true);

    } catch (JSONException js) {
        Logger.getLogger("log").logln(
            USR.ERROR,
            "JSONException in EndRouterEvent should not occur");
    }
    return json;
}

private String leadin(){
    return "EWRE: ";
}

}
