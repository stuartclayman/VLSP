package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import usr.common.*;
import us.monoid.json.*;

/** Class represents a global controller event*/
public class EndLinkEvent extends Event
{
int router1_;
int router2_;
boolean routerNumsSet_ = true;
String addr1_ = null;
String addr2_ = null;

public EndLinkEvent (long time, EventEngine eng, int r1, int r2)
{
    time_ = time;
    engine_ = eng;
    router1_ = r1;
    router2_ = r2;
}

public EndLinkEvent (long time, EventEngine eng, String add1, 
        String add2){
    time_ = time;
    engine_ = eng;
    addr1_ = add1;
    addr2_ = add2;
    routerNumsSet_ = false;
}

public EndLinkEvent (long time, EventEngine eng, String add1,
    String add2, GlobalController gc)
throws InstantiationException 
{
    time_ = time_;
    engine_ = eng;
    addr1_ = add1;
    addr2_ = add2;
    initNumbers(add1, add2, gc);
}

public String toString()
{
    String str = "EndLinkEvent " + time_ + getName();
    return str;
}

private String getName(){
    String str = "";
    if (addr1_ == null)
        str += router1_ + " " + router2_;
    else
        str += addr1_ + " " + addr2_;
    return str;
}

private void initNumbers(String add1, String add2, GlobalController gc)
throws InstantiationException 
{
    BasicRouterInfo r1Info = gc.findRouterInfo(add1);
    if (r1Info == null) throw new InstantiationException(
            "Cannot find address " + add1);
    BasicRouterInfo r2Info = gc.findRouterInfo(add2);
    if (r2Info == null) throw new InstantiationException(
            "Cannot find address " + add2);
    router1_ = r1Info.getId();
    router2_ = r2Info.getId();
}

/** Perform logic which follows an event */
public void followEvent(EventScheduler s,
    GlobalController g)                          {
    super.followEvent(s, g);
    if (g.connectedNetwork()) {
        g.connectNetwork(time_, router1_, router2_);
    } else if (!g.allowIsolatedNodes()) {
        g.checkIsolated(time_, router1_);
        g.checkIsolated(time_, router2_);
    }
}

public JSONObject execute(GlobalController gc) throws
InstantiationException {
    if (!routerNumsSet_)
        initNumbers(addr1_, addr2_, gc);
    JSONObject json = new JSONObject();
    boolean success = gc.endLink(time_, router1_, router2_);
    try {
        if (success) {
            json.put("success", true);
            json.put("msg", "Link removed " + getName());
            json.put("router1", (Integer)router1_);
            json.put("router2", (Integer)router2_);
            if (addr1_ != null) {
                json.put("address1", addr1_);
                json.put("address2", addr2_);
            }
        } else {
            json.put("success", false);
            json.put("msg", "Failed to end link");
        }
    } catch (JSONException js) {
        Logger.getLogger("log").logln(
            USR.ERROR,
            "JSONException in EndLinkEvent should not occur");
    }
    return json;
}
}
