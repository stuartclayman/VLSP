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
    String [] ctxData = {};

    public SetAggPointEvent(long time, EventEngine eng, int rid, int AP) {
        super(time, eng);
        AP_ = AP;
        routerNo_ = rid;
    }

    public SetAggPointEvent(long time, EventEngine eng, int rid, int AP, String[] ctxArgs) {
        super(time, eng);
        AP_ = AP;
        routerNo_ = rid;
        ctxData = ctxArgs;
    }

    @Override
    public String toString() {
        String str;

        str = "SetAggPointEvent: " + time + " router " + routerNo_ + " AP " + AP_;
        return str;
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject json= setAP(time,routerNo_, AP_, ctxData, gc);

        return json;
    }

    public JSONObject setAP(long time, int gid, int AP, String[] ctxArgs, GlobalController gc) {
        System.out.println("SetAggPointEvent: setAP called");

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
            if (!callSetAP(gid,AP,ctxArgs,gc)) {
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

    private boolean callSetAP (int gid, int AP, String[] ctxArgs, GlobalController gc) {
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
            lci.setAP(gid, AP, ctxArgs);

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
