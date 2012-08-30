package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import usr.interactor.*;
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
    boolean success = endLink(time_, router1_, router2_, gc);
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


/** Event to unlink two routers
 * Returns true for success*/
private boolean endLink(long time, int router1Id,
    int router2Id, GlobalController gc)
{
    // return 0 for end of link
    gc.unregisterLink(router1Id, router2Id);
    if (gc.isSimulation())
        endSimulationLink(router1Id, router2Id, gc);
    else
        return endEmulatedLink(router1Id, router2Id, gc);
    return true;
}


private void endSimulationLink(int router1Id, int router2Id,
    GlobalController gc)
{
/** Event to end simulation link between two routers */
    // Nothing need happen here right now.
}

/** Event to end emulated link between two routers */
private boolean endEmulatedLink(int rId1, int rId2,
    GlobalController gc)
{
    BasicRouterInfo br1 = gc.findRouterInfo(rId1);
    BasicRouterInfo br2 = gc.findRouterInfo(rId1);
    
    if (br1 == null || br2 == null) 
        return false;
    
    LocalControllerInteractor lci = gc.getLocalController(br1);
    int MAX_TRIES = 5;
    int i;

    for (i = 0; i < MAX_TRIES; i++) {
        try {
            lci.endLink(br1.getHost(),
                br1.getManagementPort(), br2.getAddress());
            Pair<Integer, Integer> pair = gc.makePair(rId1, rId2);
            Integer linkID = pair.hashCode();
            gc.removeLinkInfo(linkID);

            Logger.getLogger("log").logln(USR.STDOUT,
                leadin() + "remove link from: " + rId2 +
                " to " + rId1 + " with link ID: " + linkID);
          /*  Logger.getLogger("log").logln(1 << 9,
                 */
            break; 
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                leadin() + "Cannot shut down link " +
                br1.getHost() + ":" + br1.getManagementPort() + " " +
                br2.getHost() + ":" + br2.getManagementPort() +
                " try " + (i + 1));
        }
    }
    if (i == MAX_TRIES) {
        Logger.getLogger("log").logln(
            USR.ERROR, "Giving up after failure to shut link");
        return false;
    }
    return true;
}


private String leadin(){
    return "ELE: ";
}

}

