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
public class EndRouterEvent extends AbstractEvent {
    int routerNo_;
    String address_ = null;
    boolean routerNumSet_ = true;

    public EndRouterEvent(long time, EventEngine eng, String address, GlobalController gc) throws
    InstantiationException {
        time_ = time;
        engine_ = eng;
        initNumber(address, gc);
    }

    public EndRouterEvent(long time, EventEngine eng, String addr) {
        time_ = time;
        engine_ = eng;
        address_ = addr;
        routerNumSet_ = false;
    }

    public EndRouterEvent(long time, EventEngine eng, int rNo) {
        time_ = time;
        engine_ = eng;
        routerNo_ = rNo;
    }

    public String toString() {
        String str;

        str = "EndRouter: " + time_ + " " + getName();
        return str;
    }

    public int getNumber(GlobalController gc) throws InstantiationException {
        if (!routerNumSet_) {
            initNumber(address_, gc);
        }

        return routerNo_;
    }

    private String getName() {
        String str = "";

        if (address_ != null) {
            str = address_;
        }

        return str;
    }

    private void initNumber(String address, GlobalController gc) throws
    InstantiationException {
        BasicRouterInfo rInfo = gc.findRouterInfo(address);

        if (rInfo == null) {
            throw new InstantiationException(
                      "Cannot find router " + address);
        }

        routerNo_ = rInfo.getId();
    }

    public JSONObject execute(GlobalController gc) throws
    InstantiationException {
        if (!routerNumSet_) {
            initNumber(address_, gc);
        }

        boolean success = endRouter(routerNo_, gc);
        JSONObject json = new JSONObject();
        try {
            if (success) {
                json.put("success", (Boolean)true);
                json.put("msg", "Shut down router " + getName());
                json.put("router", (Integer)routerNo_);

                if (address_ != null) {
                    json.put("address", address_);
                }
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

    public void followEvent(EventScheduler s, JSONObject response, GlobalController g) {
        super.followEvent(s, response, g);

        if (g.connectedNetwork()) {
            g.connectNetwork(time_);
        } else if (!g.allowIsolatedNodes()) {
            g.checkIsolated(time_);
        }
    }

    /** Event to end a router -- returns true for success */
    public boolean endRouter(int routerId, GlobalController gc) {
        boolean success;

        if (gc.isSimulation()) {
            endSimulationRouter(routerId, gc);
            success = true;
        } else {
            success = endEmulatedRouter(routerId, gc);
        }

        if (success) {
            gc.unregisterRouter(routerId);
        }

        return success;
    }

    /** remove a router in simulation*/
    private void endSimulationRouter(int rId, GlobalController gc) {
    }

    /** Send shutdown to an emulated router */
    private boolean endEmulatedRouter(int rId, GlobalController gc) {
        BasicRouterInfo br = gc.findRouterInfo(rId);

        if (br == null) {
            return false;
        }

        LocalControllerInteractor lci = gc.getLocalController(br);
        int MAX_TRIES = 5;
        int i;

        for (i = 0; i < MAX_TRIES; i++) {
            try {
                lci.endRouter(br.getHost(), br.getManagementPort());
                break;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()
                                              + "Cannot shut down router " + br.getHost() + ":"
                                              + br.getManagementPort() + " attempt "
                                              + (i + 1) + " Exception = " + e);
            }
        }

        if (i == MAX_TRIES) {
            return false;
        }

        LocalControllerInfo lcinf = br.getLocalControllerInfo();
        PortPool pp = gc.getPortPool(lcinf);
        pp.freePort(br.getManagementPort());
        pp.freePort(br.getRoutingPort());
        lcinf.delRouter();
        gc.removeBasicRouterInfo(rId);
        gc.latticeMonitorRouterRemoval(br);
        return true;
    }

    private String leadin() {
        return "ERE: ";
    }

}