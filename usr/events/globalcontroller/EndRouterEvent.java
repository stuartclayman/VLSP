package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.common.PortPool;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.lifeEstimate.LifetimeEstimate;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndRouterEvent extends AbstractGlobalControllerEvent {
    int routerNo_;
    String address_ = null;
    boolean routerNumSet_ = true;

    public EndRouterEvent(long time, EventEngine eng, String address, GlobalController gc) throws InstantiationException {
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

    @Override
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

    private void initNumber(String address, GlobalController gc) throws InstantiationException {
        BasicRouterInfo rInfo = gc.findRouterInfo(address);

        if (rInfo == null) {
            throw new InstantiationException(
                                             "Cannot find router " + address);
        }

        routerNo_ = rInfo.getId();
    }

    @Override
    public JSONObject execute(GlobalController gc) throws InstantiationException {

        if (!routerNumSet_) {
            initNumber(address_, gc);
        }
        boolean success = endRouter(routerNo_, gc, time_);

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
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in EndRouterEvent should not occur");
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Exception in EndRouterEvent should not occur");
            e.printStackTrace();
        }

        return json;
    }

    @Override
    public void followEvent(JSONObject response, GlobalController g) {
        super.followEvent(response, g);
        // Schedule a check on network connectivity
        if (g.connectedNetwork()) {
            ConnectNetworkEvent cne= new ConnectNetworkEvent (g.getElapsedTime());
            getEventScheduler().addEvent(cne);
        } else if (!g.allowIsolatedNodes()) {
            g.getAbstractNetwork().checkIsolated(time_,g);
        }
    }

    /** Event to end a router -- returns true for success */
    public boolean endRouter(int routerId, GlobalController gc, long time) {
        boolean success;

        if (gc.isSimulation()) {
            success = endSimulationRouter(routerId, gc);
        } else {
            success = endEmulatedRouter(routerId, gc);
        }
        if (LifetimeEstimate.usingLifetimeEstimate()) {
            LifetimeEstimate.getLifetimeEstimate().nodeDeath(time, routerId);
        }

        if (success) {
            gc.unregisterRouter(time, routerId);
        }

        return success;
    }

    /** remove a router in simulation*/
    private boolean endSimulationRouter(int rId, GlobalController gc) {
    	// Actually this doesn't currently require any action
    	return true;
    }

    /** Send shutdown to an emulated router */
    private boolean endEmulatedRouter(int rId, GlobalController gc) {
        BasicRouterInfo br = gc.findRouterInfo(rId);

        if (br == null) {
            return false;
        }

        LocalControllerInteractor lci = gc.getLocalController(br);

        int MAX_TRIES = 5;
        int i = 0;
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
        gc.removeRouterInfo(rId);

        return true;
    }

    private String leadin() {
        return "ERE: ";
    }

}
