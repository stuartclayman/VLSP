package usr.events;

import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class SetAggPointEvent extends AbstractEvent {
    int routerNo_;
    int AP_;

    public SetAggPointEvent(long time, EventEngine eng, int rid, int AP) {
        time_ = time;
        AP_ = AP;
        routerNo_ = rid;
    }

    @Override
	public String toString() {
        String str;

        str = "SetAggPointEvent: " + time_ + " router " + routerNo_ + " AP " + AP_;
        return str;
    }

    @Override
	public JSONObject execute(GlobalController gc) {
        JSONObject json= setAP(routerNo_, AP_, gc);

        return json;
    }

    public static JSONObject setAP(int gid, int AP, GlobalController gc) {
        //System.out.println("setAP called");
        
        JSONObject json= new JSONObject();
        
        try {
            json.put("success", (Boolean)false);
            json.put("msg", "SetAggPoint Event "+gid+" "+AP);
            json.put("gid",gid);
            json.put("AP",AP);
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in SetAggPointEvent should not occur");
        }
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + " router " + gid + " now has access point " + AP);

        BasicRouterInfo br = gc.findRouterInfo(gid);

        if (br == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to find router " + gid
                                          + " in router map");
            return json;
        }

        LocalControllerInteractor lci = gc.getLocalController(br);

        if (lci == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to find router " + gid
                                          + " in interactor map");
            return json;
        }

        try {
            lci.setAP(gid, AP);

            // save data
            gc.registerAggPoint(gid, AP);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to set AP for router " + gid);
        }
        try {
            json.put("success", (Boolean)true);
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in SetAggPointEvent should not occur");
        }
        return json;
    }

    private static String leadin() {
        return "SetAggPt: ";
    }


}
