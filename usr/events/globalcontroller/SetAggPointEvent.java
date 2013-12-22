package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class SetAggPointEvent extends AbstractGlobalControllerEvent {
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
        JSONObject json= setAP(time_,routerNo_, AP_, gc);

        return json;
    }

    public JSONObject setAP(long time, int gid, int AP, GlobalController gc) {
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
        if (!gc.isSimulation()) {
            if (!callSetAP(gid,AP,gc)) {
                try {
                    json.put("success", (Boolean)false);
                } catch (Exception e) {
                    Logger.getLogger("log").logln(
                                                  USR.ERROR,
                                                  "JSONException in SetAggPointEvent should not occur");
                }
                return json;
            }
        }
        gc.registerAggPoint(time,gid, AP);
        try {
            json.put("success", (Boolean)true);
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                                          USR.ERROR,
                                          "JSONException in SetAggPointEvent should not occur");
        }
        return json;
    }

    private boolean callSetAP (int gid, int AP, GlobalController gc) {
        BasicRouterInfo br = gc.findRouterInfo(gid);

        if (br == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to find router " + gid
                                          + " in router map");
            return false;
        }

        LocalControllerInteractor lci = gc.getLocalController(br);

        if (lci == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to find router " + gid
                                          + " in interactor map");
            return false;
        }

        try {
            lci.setAP(gid, AP);

            // save data

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to set AP for router " + gid);
            return false;
        }
        return true;
    }


    private String leadin() {
        return "SetAggPt: ";
    }


}
