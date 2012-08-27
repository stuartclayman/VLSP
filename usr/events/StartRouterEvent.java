package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import us.monoid.json.*;
import usr.common.*;

/** Class represents a global controller event*/
public class StartRouterEvent extends Event
{
String address_ = null;
String name_ = null;

public StartRouterEvent (long time, EventEngine eng){
    time_ = time;
    engine_ = eng;
}

public StartRouterEvent (long time,
    EventEngine eng,
    String address,
    String name) throws
InstantiationException {
    time_ = time;
    engine_ = eng;
    name_ = name;
    address_ = address;
}

public String toString(){
    String str;

    str = "StartRouter: " + time_ + " " + nameString();
    return str;
}

private String nameString(){
    String str = "";

    if (name_ != null)
        str += " " + name_;
    if (address_ != null)
        str += " " + address_;
    return str;
}

public JSONObject execute(GlobalController gc) throws
InstantiationException {
    int rNo = gc.startRouter(time_, address_, name_);
    JSONObject jsobj = new JSONObject();

    try {
        if (rNo < 0) {
            jsobj.put("success", false);
            jsobj.put("msg", "Could not create router");
        } else {
            BasicRouterInfo bri= gc.findRouterInfo(rNo);
            jsobj.put("success", true);
            jsobj.put("routerID", bri.getId());
            jsobj.put("name", bri.getName());
            jsobj.put("address", bri.getAddress());
            jsobj.put("mgmtPort", bri.getManagementPort());
            jsobj.put("r2rPort", bri.getRoutingPort());
            jsobj.put("msg", "Created router " + rNo + " " +
                bri.getName()+":"+bri.getRoutingPort());
        }
    } catch (JSONException je) {
        Logger.getLogger("log").logln(
            USR.ERROR,
            "JSONException in StartLinkEvent should not occur");
    }
    return jsobj;
}
}
