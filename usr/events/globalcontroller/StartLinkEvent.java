package usr.events.globalcontroller;

import java.io.IOException;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.abstractnetwork.AbstractLink;
import usr.common.BasicRouterInfo;
import usr.common.LinkInfo;
import usr.common.Pair;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartLinkEvent extends AbstractGlobalControllerEvent {
    private int router1_;
    private int router2_;
    private int weight_ = 1;
    private String address1_ = null;
    private String address2_ = null;
    private String linkName_ = null;
    private boolean numbersSet_ = false;
    private boolean scheduled_= false;

    public StartLinkEvent(long time, EventEngine eng, int r1, int r2) {
        super(time, eng);
        router1_ = r1;
        router2_ = r2;
        numbersSet_ = true;
    }

    public StartLinkEvent(long time, EventEngine eng, AbstractLink link) {
        super(time, eng);
        router1_ = link.getNode1();
        router2_ = link.getNode2();
        numbersSet_ = true;
    }

    public StartLinkEvent(long time, EventEngine eng, int r1, int r2, int w) {
        super(time, eng);
        router1_ = r1;
        router2_ = r2;
        weight_ = w;
        numbersSet_ = true;
    }

    public StartLinkEvent(long time, EventEngine eng, String add1, String add2) {
        super(time, eng);
        address1_ = add1;
        address2_ = add2;
        numbersSet_ = false;
    }

    public StartLinkEvent(long time, EventEngine eng, String add1, String add2, GlobalController gc) throws InstantiationException {
        super(time, eng);
        address1_ = add1;
        address2_ = add2;
        setRouterNumbers(add1, add2, gc);
        numbersSet_ = true;
    }

    /**
     * Create a StartLinkEvent from an existing generic StartLinkEvent
     */
    public StartLinkEvent(usr.events.vim.StartLinkEvent sle) {
        super(sle.time, sle.engine);

        if (sle.address1 == null) {   // address is null, so use routerNo
            router1_ = sle.router1;
            router2_ = sle.router2;
            numbersSet_ = true;
        } else {
            address1_ = sle.address1;
            address2_ = sle.address2;
            numbersSet_ = false;
        }


        weight_ =  sle.getWeight();
        linkName_ = sle.getLinkName();

    }


    public int getRouter1(GlobalController gc) throws InstantiationException {
        if (!numbersSet_) {
            setRouterNumbers(address1_, address2_, gc);
        }

        return router1_;
    }

    public int getRouter2(GlobalController gc) throws InstantiationException {
        if (!numbersSet_) {
            setRouterNumbers(address1_, address2_, gc);
        }

        return router2_;
    }

    @Override
    public String toString() {
        String str = "StartLink " + time + " " + getName();

        return str;
    }

    private String getName() {
        String str = "";

        if (numbersSet_) {
            str += router1_ + " " + router2_ + " ";
        }

        if (address1_ != null) {
            str += address1_ + " " + address2_;
        }

        return str;
    }

    private void setRouterNumbers(String add1, String add2, GlobalController gc)
        throws InstantiationException {
        BasicRouterInfo r1Info = gc.findRouterInfo(add1);

        if (r1Info == null) {
            throw new InstantiationException(
                                             "Cannot find address " + add1);
        }

        BasicRouterInfo r2Info = gc.findRouterInfo(add2);

        if (r2Info == null) {
            throw new InstantiationException(
                                             "Cannot find address " + add2);
        }

        router1_ = r1Info.getId();
        router2_ = r2Info.getId();
        numbersSet_ = true;
    }

    public void setWeight(int w) {
        weight_ = w;
    }

    public void setName(String s) {
        linkName_ = s;
    }

    @Override
    public JSONObject execute(GlobalController gc) throws InstantiationException {
        if (!numbersSet_) {
            setRouterNumbers(address1_, address2_, gc);
        }

        JSONObject json = new JSONObject();
        int r1 = router1_;
        int r2 = router2_;

        if (!gc.isRouterAlive(r1)) {
            try {
                json.put("success", (Boolean)false);
                json.put("msg", "Router not alive " + r1);
                return json;
            } catch (JSONException js) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "JSONException in StartLinkEvent should not occur");
            }
        }

        if (!gc.isRouterAlive(r2)) {
            try {
                json.put("success", (Boolean)false);
                json.put("msg", "Router not alive " + r2);
                return json;
            } catch (JSONException js) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "JSONException in StartLinkEvent should not occur");
            }
        }

        int linkNo = startLink(gc, time, r1, r2, weight_, linkName_, scheduled_);
        boolean success = linkNo >= 0;
        try {
            if (success) {
                json.put("success", (Boolean)true);
                json.put("msg", "link started " + getName());
                json.put("router1", (Integer)r1);
                json.put("router2", (Integer)r2);
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

    /** Event to link two routers  Return -1 for fail or link id*/
    public int startLink(GlobalController gc, long time, int router1Id, int router2Id,
                                int weight, String name, boolean scheduled) {
        // check if this link already exists
        int [] outForRouter1 = gc.getOutLinks(router1Id);
        boolean gotIt = false;

        for (int i : outForRouter1) {
            if (i == router2Id) {
                gotIt = true;
                break;
            }
        }

        if (gotIt) {         // we already have this link
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Link already exists: "
                                          + router1Id + " -> " + router2Id);
            return -1;
        } else {
            int linkID;

            if (gc.isSimulation()) {
                linkID = startSimulationLink(gc, router1Id, router2Id);
            } else {
                linkID = startVirtualLink(gc, router1Id, router2Id, weight,
                                          name);
            }

            // register inside GlobalController
            gc.registerLink(time,router1Id, router2Id, scheduled);

            return linkID;
        }
    }

    /** Start simulation link */
    private int startSimulationLink(GlobalController gc, int router1Id, int router2Id) {
        return 0;
    }

    /**
     * Send commands to start virtual link
     * Args are: router1 ID, router2 ID, the weight for the link, a name for the link
     */
    private int startVirtualLink(GlobalController gc, int router1Id, int router2Id, int weight, String name) {
        BasicRouterInfo br1, br2;
        LocalControllerInfo lc;
        LocalControllerInteractor lci;

        br1 = gc.findRouterInfo(router1Id);
        br2 = gc.findRouterInfo(router2Id);

        if (br1 == null) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Router " + router1Id
                                          + " does not exist when trying to link to "
                                          + router2Id);
            return -1;
        }

        if (br2 == null) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Router " + router2Id
                                          + " does not exist when trying to link to "
                                          + router1Id);
            return -1;
        }

        lc = br1.getLocalControllerInfo();
        lci = gc.getLocalController(lc);
        Logger.getLogger("log").logln(USR.STDOUT, leadin()
                                      + "Global controller linking routers "
                                      + br1 + " and " + br2);
        int MAX_TRIES = 5;
        int i;
        Integer linkID = -1;

        for (i = 0; i < MAX_TRIES; i++) {
            try {
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Asking for lci response to link routers");
                JSONObject response = lci.connectRouters(br1.getHost(), br1.getManagementPort(),
                                                         br2.getHost(), br2.getManagementPort(),
                                                         weight, name);
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Got lci response:"+response);
                String connectionName = (String)response.get("name");

                // add Pair<router1Id, router2Id> -> connectionName to  linkNames
                Pair<Integer, Integer> endPoints = GlobalController.makePair(router1Id, router2Id);
                linkID = endPoints.hashCode();

                gc.setLinkInfo(linkID, new LinkInfo(endPoints, connectionName, weight, linkID, gc.getElapsedTime()));

                Logger.getLogger("log").logln(USR.STDOUT,
                                              leadin() + br1 + " -> " + br2 + " = "
                                              + connectionName + " with link ID: " + linkID);
                break;
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin()
                                              + "Cannot link routers "
                                              + router1Id + " " + router2Id
                                              + " try " + (i + 1));
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin() + e.getMessage());
            } catch (JSONException e) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin() + "Cannot link routers "
                                              + router1Id + " " + router2Id
                                              + " try " + (i + 1));
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin() + e.getMessage());
            }
        }

        if (i == MAX_TRIES) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Giving up on linking");
            gc.shutDown();
        }

        return linkID;
    }

    public void setScheduled()
    {
        scheduled_= true;
    }

    public boolean isScheduled()
    {
        return scheduled_;
    }

    private String leadin() {
        return "GC(SLE):";
    }

}
