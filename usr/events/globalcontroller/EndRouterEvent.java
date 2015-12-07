package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.vim.EndRouter;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.common.PortPool;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.model.lifeEstimate.LifetimeEstimate;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndRouterEvent extends AbstractGlobalControllerEvent implements EndRouter {
    int address_;
    String name_ = null;
    boolean routerNumSet_ = true;

    public EndRouterEvent(long time, EventEngine eng, String name, GlobalController gc) throws InstantiationException {
        super(time, eng);
        initNumber(name, gc);
    }

    public EndRouterEvent(long time, EventEngine eng, String nm) {
        super(time, eng);
        name_ = nm;
        routerNumSet_ = false;
    }

    public EndRouterEvent(long time, EventEngine eng, int rNo) {
        super(time, eng);
        address_ = rNo;
    }

    /**
     * Create a EndRouterEvent from an existing generic EndRouterEvent.
     */
    public EndRouterEvent(usr.events.vim.EndRouterEvent ere) {
        super(ere.time, ere.engine);

        if (ere.name == null) {  // name is null, so use address
            address_ = ere.address;
        } else {
            name_ = ere.name;
        }
    }

    @Override
    public String toString() {
        String str;

        str = "EndRouter: " + time + " " + getName();
        return str;
    }

    public int getNumber(GlobalController gc) throws InstantiationException {
        if (!routerNumSet_) {
            initNumber(name_, gc);
        }

        return address_;
    }

    private String getName() {
        String str = "";

        if (name_ != null) {
            str = name_;
        }

        return str;
    }

    private void initNumber(String name, GlobalController gc) throws InstantiationException {
        BasicRouterInfo rInfo = gc.findRouterInfo(name);

        if (rInfo == null) {
            throw new InstantiationException("Cannot find router " + name);
        }

        address_ = rInfo.getId();
    }

    @Override
    public JSONObject execute(GlobalController gc) {

        JSONObject json = new JSONObject();
        try {

            if (!routerNumSet_) {
                initNumber(name_, gc);
            }
            boolean success = endRouter(address_, gc, time);

            if (success) {
                json.put("success", (Boolean)true);
                json.put("msg", "Shut down router " + getName());
                json.put("router", (Integer)address_);

                if (name_ != null) {
                    json.put("name", name_);
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
        // Schedule a check on network connectivity
        if (g.connectedNetwork()) {
            EventScheduler scheduler = getEventScheduler();
            if (scheduler != null) {
                ConnectNetworkEvent cne= new ConnectNetworkEvent (g.getElapsedTime());
                scheduler.addEvent(cne);
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "EndRouterEvent followEvent() cannot access EventScheduler");
            }
        } else if (!g.allowIsolatedNodes()) {
            g.getAbstractNetwork().checkIsolated(time);
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
        lcinf.delRouter(rId);
        gc.removeRouterInfo(rId);

        return true;
    }

    private String leadin() {
        return "ERE: ";
    }

}
