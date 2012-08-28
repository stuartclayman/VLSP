package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import usr.common.*;
import us.monoid.json.*;

/** Class represents an event which lists all links*/
public class ListLinksEvent extends Event
{

public ListLinksEvent (long time, EventEngine eng){
    time_ = time;
    engine_ = eng;
}


public String toString(){
    String str = "ListLinks " + time_ + " ";

    return str;
}


public JSONObject execute(GlobalController gc) 
throws InstantiationException 
{
    JSONObject jsobj= null;
    try {
        jsobj = new JSONObject();
        JSONArray array = new JSONArray();
        for (LinkInfo info : gc.getAllLinkInfo())
            array.put(info.getLinkID());

        jsobj.put("type", "link");
        jsobj.put("list", array);
        return jsobj;
    } catch (JSONException js) {
        Logger.getLogger("log").logln(
            USR.ERROR,
            "JSONException in ListLinksEvent should not occur");
    }
    return jsobj;
}

}
