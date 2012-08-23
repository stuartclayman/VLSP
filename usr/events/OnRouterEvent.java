package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import usr.common.*;
import us.monoid.json.*;

/** Class represents a global controller event*/
public class OnRouterEvent extends Event
{
int routerNo_ = 0;
String className_ = null;
String [] command_ = null;
String address_ = null;
boolean routerNumSet_ = true;

public OnRouterEvent (long time,
    EventEngine eng,
    int rNo,
    String cname,
    String []   args){
    time_ = time;
    engine_ = eng;
    routerNo_ = rNo;
    className_ = cname;
    command_ = args;
}

public OnRouterEvent (long time,
    EventEngine eng,
    String addr,
    String cname,
    String []   args){
    time_ = time;
    engine_ = eng;
    className_ = cname;
    address_ = addr;
    command_ = args;
    routerNumSet_ = false;
}

public OnRouterEvent (long time,
    EventEngine eng,
    String addr,
    String cname,
    String []        args,
    GlobalController gc)
throws InstantiationException {
    time_ = time;
    engine_ = eng;
    className_ = cname;
    command_ = args;
    address_ = addr;
    setRouterNo(addr, gc);
}

public String toString(){
    String str = "OnRouter " + time_ + getName();

    return str;
}
private String getName(){
    String str = "";

    if (address_ == null)
        str += (routerNo_ + " ");
    else
        str += (address_ + " ");
    str += className_ + " Command:";
    for (String a : command_)
        str += " " + a;
    return str;
}

private void setRouterNo(String addr, GlobalController gc) throws
InstantiationException {
    BasicRouterInfo rInfo = gc.findRouterInfo(addr);

    if (rInfo == null) throw new InstantiationException(
            "Cannot find address " + addr);
    routerNo_ = rInfo.getId();
}

public JSONObject execute(GlobalController gc) throws
InstantiationException {
    if (!routerNumSet_)
        setRouterNo(address_, gc);
    int start = gc.appStart(routerNo_, className_, command_);
    JSONObject json = new JSONObject();
    try {
        if (start >= 0) {
            json.put("success", true);
            json.put("msg",
                "Started Application on router " + getName());
        } else {
            json.put("success", (Boolean)false);
            json.put("msg",
                "Unable to start application on router " +
                getName());
        }
    } catch (JSONException e) {
        Logger.getLogger("log").logln(
            USR.ERROR,
            "JSONException in CreateTrafficEvent should not occur");
    }
    return json;
}
}