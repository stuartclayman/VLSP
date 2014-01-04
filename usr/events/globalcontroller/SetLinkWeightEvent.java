package usr.events.globalcontroller;

import java.io.IOException;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.ANSI;
import usr.common.BasicRouterInfo;
import usr.common.LinkInfo;
import usr.common.Pair;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class SetLinkWeightEvent extends AbstractGlobalControllerEvent {
    private int router1_;
    private int router2_;
    private int weight_;


    public SetLinkWeightEvent(long time, EventEngine eng, int r1, int r2, int weight) {
        super(time, eng);
        router1_ = r1;
        router2_ = r2;
        weight_ = weight;
    }

    @Override
    public String toString() {
        String str = "SetLinkWeightEvent " + time + " " + router1_ + " " + router2_ + " " + weight_;

        return str;
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        int linkID = setLinkWeight(router1_, router2_, weight_, gc);

        JSONObject jsobj = new JSONObject();


        try {
            if (linkID != 0) {
                jsobj = gc.getLinkInfo(linkID);
                jsobj.put("success", true);
            } else {
                jsobj.put("success", false);
                jsobj.put("msg", "Could not set link weight");
            }

        } catch (JSONException js) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in SetLinkWeightEvent should not occur");
        }

        return jsobj;

    }

    /**
     * Set the weight on a link
     * Returns linkID
     */
    protected int setLinkWeight(int router1Id, int router2Id, int weight, GlobalController gc) {

        BasicRouterInfo br1, br2;
        LocalControllerInteractor lci1, lci2;


        br1 = gc.findRouterInfo(router1Id);
        br2 = gc.findRouterInfo(router2Id);

        if (br1 == null) {
            System.err.println ("Router "+router1Id+" does not exist when trying to set weight to "+ router2Id);
            return -1;
        }

        if (br2 == null) {
            System.err.println ("Router "+router2Id+" does not exist when trying to set weight to "+ router1Id);
            return -1;
        }
        //Logger.getLogger("log").logln(USR.STDOUT, "Got router Ids"+br1.getHost()+br2.getHost());

        br1.getLocalControllerInfo();
        br2.getLocalControllerInfo();
        //Logger.getLogger("log").logln(USR.STDOUT, "Got LC");
        lci1 = gc.getLocalController(br1);
        lci2 = gc.getLocalController(br2);
        //Logger.getLogger("log").logln(USR.STDOUT, "Got LCI");
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Global controller setting weight on link between routers "+
                                      br1 + " and "+ br2 + " to " + weight);

        try {
            // set weight at router1 end
            lci1.setLinkWeight(br1.getHost(), br1.getManagementPort(), br2.getAddress(), weight);
            // set weight at router2 end
            lci2.setLinkWeight(br2.getHost(), br2.getManagementPort(), br1.getAddress(), weight);
        } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "Cannot set weight on link between routers "+router1Id+" "+router2Id);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());

                return 0;

            } catch (JSONException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "Cannot set weight on link between routers "+router1Id+" "+router2Id);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());

                return 0;
            }

        // TODO: dont foreget to update linkInfo object for the relevant
        // link with the new weight

        // Create Pair<router1Id, router2Id>
        Pair<Integer, Integer> endPoints = gc.makePair(router1Id, router2Id);
        // and determine linkID
        int linkID = endPoints.hashCode();

        LinkInfo lInfo = gc.findLinkInfo(linkID);

        if (lInfo == null) {
            Logger.getLogger("log").logln(USR.ERROR, ANSI.RED + leadin() + "setLinkWeight: Cannot convert routerIDs to LinkInfo" + ANSI.RESET_COLOUR);
        } else {
            lInfo.setLinkWeight(weight);
        }

        return linkID;
    }


    private String leadin() {
        return "GC(SLWE):";
    }


}
