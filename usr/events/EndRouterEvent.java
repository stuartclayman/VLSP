package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import usr.common.*;
import us.monoid.json.*;

/** Class represents a global controller event*/
public class EndRouterEvent extends Event
{
int routerNo_;
String address_ = null;
boolean routerNumSet_ = true;

public EndRouterEvent (long time, EventEngine eng, String address,
        GlobalController gc) throws
        InstantiationException 
{
    time_ = time;
    engine_ = eng;
    initNumber(address, gc);
}

public EndRouterEvent (long time, EventEngine eng, String addr)
{
    time_ = time;
    engine_ = eng;
    address_ = addr;
    routerNumSet_ = false;
}

public EndRouterEvent (long time, EventEngine eng, int rNo){
    time_ = time;
    engine_ = eng;
    routerNo_ = rNo;
}

public String toString(){
    String str;

    str = "EndRouter: " + time_ + " " + getName();
    return str;
}

private String getName(){
    String str = "";

    if (address_ != null)
        str = address_;
    return str;
}

private void initNumber(String address, GlobalController gc) throws
InstantiationException {
    BasicRouterInfo rInfo = gc.findRouterInfo(address);

    if (rInfo == null) throw new InstantiationException(
            "Cannot find router " + address);
    routerNo_ = rInfo.getId();
}

public JSONObject execute(GlobalController gc) throws
InstantiationException {
    if (!routerNumSet_)
        initNumber(address_, gc);
    boolean success = gc.endRouter(time_, routerNo_);
    JSONObject json = new JSONObject();
    try {
        if (success) {
            json.put("success", (Boolean)true);
            json.put("msg", "Shut down router " + getName());
            json.put("router", (Integer)routerNo_);
            if (address_ != null)
                json.put("address", address_);
        } else {
            json.put("success", (Boolean)false);
            json.put("msg", "Could not shut down router " + getName());
        }
    } catch (JSONException js) {
        Logger.getLogger("log").logln(
            USR.ERROR,
            "JSONException in EndRouterEvent should not occur");
    }
    return json;
}

public void followEvent(EventScheduler s,
    GlobalController g)                          {
    super.followEvent(s, g);
    if (g.connectedNetwork())
        g.connectNetwork(time_);
    else if (!g.allowIsolatedNodes())
        g.checkIsolated(time_);
}
}
