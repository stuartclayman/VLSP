package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.vim.EndLink;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.common.Pair;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndLinkEvent extends AbstractGlobalControllerEvent implements EndLink {
    int address1_;
    int address2_;
    boolean routerNumsSet_ = true;
    String name1_ = null;
    String name2_ = null;

    public EndLinkEvent(long time, EventEngine eng, int r1, int r2) {
        super(time, eng);
        address1_ = r1;
        address2_ = r2;
    }

    public EndLinkEvent(long time, EventEngine eng, String add1, String add2) {
        super(time, eng);
        name1_ = add1;
        name2_ = add2;
        routerNumsSet_ = false;
    }

    public EndLinkEvent(long time, EventEngine eng, String add1, String add2, GlobalController gc) throws InstantiationException {
        super(time, eng);
        name1_ = add1;
        name2_ = add2;
        initNumbers(add1, add2, gc);
    }


    /**
     * Create a EndLinkEvent from an existing generic EndLinkEvent
     */
    public EndLinkEvent(usr.events.vim.EndLinkEvent ele) {
        super(ele.time, ele.engine);

        if (ele.name1 == null) {   // name is null, so use addressNo
            address1_ = ele.address1;
            address2_ = ele.address2;
            routerNumsSet_ = true;
        } else {
            name1_ = ele.name1;
            name2_ = ele.name2;
            routerNumsSet_ = false;
        }
    }


    public int getRouter1(GlobalController gc) throws InstantiationException {
        if (!routerNumsSet_) {
            initNumbers(name1_, name2_, gc);
        }

        return address1_;
    }

    public int getRouter2(GlobalController gc) throws InstantiationException {
        if (!routerNumsSet_) {
            initNumbers(name1_, name2_, gc);
        }

        return address2_;
    }

    @Override
    public String toString() {
        String str = "EndLinkEvent " + time + getName();

        return str;
    }

    private String getName() {
        String str = " ";

        if (name1_ == null) {
            str += address1_ + " " + address2_;
        } else {
            str += name1_ + " " + name2_;
        }

        return str;
    }

    private void initNumbers(String add1, String add2, GlobalController gc)
        throws InstantiationException {
        BasicRouterInfo r1Info = gc.findRouterInfo(add1);

        if (r1Info == null) {
            throw new InstantiationException(
                                             "Cannot find name " + add1);
        }

        BasicRouterInfo r2Info = gc.findRouterInfo(add2);

        if (r2Info == null) {
            throw new InstantiationException(
                                             "Cannot find name " + add2);
        }

        address1_ = r1Info.getId();
        address2_ = r2Info.getId();
    }

    /** Perform logic which follows an event */
    @Override
    public void followEvent(JSONObject response, GlobalController g) {
        if (g.connectedNetwork()) {
            ConnectNetworkEvent cne= new ConnectNetworkEvent(address1_,address2_,time);
            getEventScheduler().addEvent(cne);
        } else if (!g.allowIsolatedNodes()) {
            CheckIsolatedEvent ce = new CheckIsolatedEvent(address1_, time);
            getEventScheduler().addEvent(ce);
            ce = new CheckIsolatedEvent(address2_, time);
            getEventScheduler().addEvent(ce);
        }
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        try {
            if (!routerNumsSet_) {
                initNumbers(name1_, name2_, gc);
            }
        } catch (InstantiationException ie) {
            return fail(ie.getMessage());
        }


        JSONObject json = new JSONObject();
        boolean success = endLink(time, address1_, address2_, gc);
        try {
            if (success) {
                json.put("success", true);
                json.put("msg", "Link removed " + getName());
                json.put("router1", (Integer)address1_);
                json.put("router2", (Integer)address2_);

                if (name1_ != null) {
                    json.put("name1", name1_);
                    json.put("name2", name2_);
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
    private boolean endLink(long time, int router1Id, int router2Id, GlobalController gc) {
       
        if (gc.isSimulation()) {
            endSimulationLink(router1Id, router2Id, gc);
        } else {
            endEmulatedLink(router1Id, router2Id, gc);
        }

        // return 0 for end of link
        gc.unregisterLink(time, router1Id, router2Id);

        return true;
    }

    private void endSimulationLink(int router1Id, int router2Id, GlobalController gc) {
        /** Event to end simulation link between two routers */

        // Nothing need happen here right now.
    }

    /** Event to end emulated link between two routers */
    private boolean endEmulatedLink(int rId1, int rId2, GlobalController gc) {
        BasicRouterInfo br1 = gc.findRouterInfo(rId1);
        BasicRouterInfo br2 = gc.findRouterInfo(rId2);

        if ((br1 == null) || (br2 == null)) {
            return false;
        }

        LocalControllerInteractor lci = gc.getLocalController(br1);
        int MAX_TRIES = 5;
        int i;

        for (i = 0; i < MAX_TRIES; i++) {
            try {
                lci.endLink(br1.getHost(), br1.getManagementPort(), br2.getAddress());
                Pair<Integer, Integer> pair = gc.makePair(rId1, rId2);
                Integer linkID = pair.hashCode();
                gc.removeLinkInfo(linkID);

                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + "remove link from: " + rId2
                                              + " to " + rId1 + " with link ID: " + linkID);

                /*  Logger.getLogger("log").logln(1 << 9,
                 */
                break;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin() + "Cannot shut down link "
                                              + br1.getHost() + ":" + br1.getManagementPort() + " "
                                              + br2.getHost() + ":" + br2.getManagementPort()
                                              + " try " + (i + 1));
            }
        }

        if (i == MAX_TRIES) {
            Logger.getLogger("log").logln(
                                          USR.ERROR, "Giving up after failure to shut link");
            return false;
        }

        return true;
    }

    private String leadin() {
        return "ELE: ";
    }

}
