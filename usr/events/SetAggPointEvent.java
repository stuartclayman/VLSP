package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.localcontroller.*;
import usr.engine.*;
import usr.common.*;
import us.monoid.json.*;
import usr.interactor.*;

/** Class represents a global controller event*/
public class SetAggPointEvent extends AbstractEvent {
    int routerNo_;
    int AP_;

    public SetAggPointEvent(long time, EventEngine eng, int rid, int AP) {
        time_ = time;
        AP_ = AP;
        routerNo_ = rid;
    }

    public String toString() {
        String str;

        str = "SetAggPointEvent: " + time_ + " router " + routerNo_ + " AP " + AP_;
        return str;
    }

    public JSONObject execute(GlobalController gc) {
        JSONObject json = new JSONObject();

        setAP(routerNo_, AP_, gc);

        return json;
    }

    public void setAP(int gid, int AP, GlobalController gc) {
        //System.out.println("setAP called");
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + " router " + gid + " now has access point " + AP);

        BasicRouterInfo br = gc.findRouterInfo(gid);

        if (br == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to find router " + gid
                                          + " in router map");
            return;
        }

        LocalControllerInteractor lci = gc.getLocalController(br);

        if (lci == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to find router " + gid
                                          + " in interactor map");
            return;
        }

        try {
            lci.setAP(gid, AP);

            // save data
            gc.registerAggPoint(gid, AP);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " unable to set AP for router " + gid);
        }
    }

    private String leadin() {
        return "SetAggPt: ";
    }


}
