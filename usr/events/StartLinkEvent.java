package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import usr.common.*;
import us.monoid.json.*;

/** Class represents a global controller event*/
public class StartLinkEvent extends Event
{
private int router1_;
private int router2_;
private int weight_ = 1;
private String address1_ = null;
private String address2_ = null;
private String linkName_ = null;
private boolean numbersSet_ = false;

public StartLinkEvent (long time, EventEngine eng, int r1, int r2){
    time_ = time;
    engine_ = eng;
    router1_ = r1;
    router2_ = r2;
    numbersSet_ = true;
}

public StartLinkEvent (long time, EventEngine eng, String add1, 
        String add2)
{
    time_ = time_;
    engine_ = eng;
    address1_ = add1;
    address2_ = add2;
    numbersSet_ = false;
}

public StartLinkEvent (long time, EventEngine eng, String add1, 
    String add2, GlobalController gc)
throws InstantiationException 
{
    time_ = time_;
    engine_ = eng;
    address1_ = add1;
    address2_ = add2;
    setRouterNumbers(add1, add2, gc);
    numbersSet_ = true;
}

public String toString(){
    String str = "StartLink " + time_ + " " + getName();

    return str;
}

private String getName(){
    String str = "";

    if (numbersSet_)
        str += router1_ + " " + router2_ + " ";
    if (address1_ != null)
        str += address1_ + " " + address2_;
    return str;
}

private void setRouterNumbers(String add1, String add2,
    GlobalController gc)
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
    numbersSet_ = true;
}

public void setWeight(int w){
    weight_ = w;
}

public void setName(String s){
    linkName_ = s;
}

public JSONObject execute(GlobalController gc) throws
InstantiationException {
    if (!numbersSet_)
        setRouterNumbers(address1_, address2_, gc);
    int linkNo = gc.startLink(time_, router1_, router2_, weight_,
        linkName_);
    boolean success = linkNo >= 0;
    JSONObject json = new JSONObject();
    try {
        if (success) {
            json.put("success", (Boolean)true);
            json.put("msg", "link started " + getName());
            json.put("router1", (Integer)router1_);
            json.put("router2", (Integer)router2_);
            json.put("linkID", (Integer)linkNo);
            json.put("weight", weight_);
            if (address1_ != null) {
                json.put("address1", address1_);
                json.put("address2", address2_);
            }
            if (linkName_ != null) {
                json.put("linkName", linkName_); 
            } else if (!gc.isSimulation()) {
                LinkInfo li = gc.findLinkInfo(linkNo);
                json.put("linkName", li.getLinkName());
            }
        } else {
            json.put("success", (Boolean)false);
            json.put("msg", "Could not start link " + getName());
        }
    } catch (JSONException js) {
        Logger.getLogger("log").logln(
            USR.ERROR,
            "JSONException in StartLinkEvent should not occur");
    }
    return json;
}
}
