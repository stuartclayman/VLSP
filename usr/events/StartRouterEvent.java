package usr.events;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.common.PortPool;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartRouterEvent extends AbstractEvent {
    String address_ = null;
    String name_ = null;

    public StartRouterEvent(long time, EventEngine eng) {
        time_ = time;
        engine_ = eng;
    }

    public StartRouterEvent(long time, EventEngine eng, String address, String name) throws
    InstantiationException {
        time_ = time;
        engine_ = eng;
        name_ = name;
        address_ = address;
    }

    public String toString() {
        String str;

        str = "StartRouter: " + time_ + " " + nameString();
        return str;
    }

    private String nameString() {
        String str = "";

        if (name_ != null) {
            str += " " + name_;
        }

        if (address_ != null) {
            str += " " + address_;
        }

        return str;
    }

    public JSONObject execute(GlobalController gc) throws
    InstantiationException {
        int rNo = startRouter(gc, time_, address_, name_);
        JSONObject jsobj = new JSONObject();

        try {
            if (rNo < 0) {
                jsobj.put("success", false);
                jsobj.put("msg", "Could not create router");
            } else {
                jsobj.put("success", true);
                jsobj.put("routerID", rNo);

                if (!gc.isSimulation()) {
                    BasicRouterInfo bri = gc.findRouterInfo(rNo);
                    jsobj.put("name", bri.getName());
                    jsobj.put("address", bri.getAddress());
                    jsobj.put("mgmtPort", bri.getManagementPort());
                    jsobj.put("r2rPort", bri.getRoutingPort());
                }

                jsobj.put("msg", "Created router " + rNo + " "
                          + nameString());
            }
        } catch (JSONException je) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in StartLinkEvent should not occur");
        }

        return jsobj;
    }

    public static int startRouter(GlobalController gc, long time, String address, String name) {
        int rId = gc.getNextNodeId();

        if (gc.isSimulation()) {
            return rId;
        }

        if (doRouterStart(gc, rId, address, name) == false) {
            //  System.err.println("Did not start");
            // gc.unregisterRouter(rId);
            return -1;
        }

        gc.registerRouter(rId);
        gc.addAPNode(time, rId);

        return rId;
    }

    private static boolean doRouterStart(GlobalController gc, int id, String address, String name) {
        // If we have no address or name fake these
        if (name == null) {
            name = new String("Router-" + id);
        }

        if (address == null) {
            address = new String("" + id);
        }

        // Find least used local controller
        LocalControllerInfo leastUsed = gc.getLeastUsedLC();

        if (leastUsed == null) {
            return false;
        }

        leastUsed.addRouter(); // Increment count
        LocalControllerInteractor lci = gc.getLocalController(leastUsed);
        int MAX_TRIES = 5;

        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                if (tryRouterStart(gc, id, address, name, leastUsed,
                                   lci)) {
                    //System.err.println("Started");
                    return true;
                }
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin()
                                              + "Could not start new router on "
                                              + leastUsed + " out of ports ");

                //System.err.println("Out of ports");
                return false;
            }
        }

        Logger.getLogger("log").logln(USR.ERROR,
                                      leadin() + "Could not start new router on "
                                      + leastUsed + " after " + MAX_TRIES + " tries.");

        //System.err.println("Could not start");
        return false;
    }

    /** Make one attempt to start a router */
    private static boolean tryRouterStart(GlobalController gc, int id, String address, String name, LocalControllerInfo local,
                                          LocalControllerInteractor lci)
    throws IOException {
        int port = 0;
        PortPool pp = gc.getPortPool(local);
        JSONObject routerAttrs;

        try {
            // find 2 ports
            port = pp.findPort(2);

            Logger.getLogger("log").logln(USR.STDOUT, leadin()
                                          + "Creating router: " + id
                                          + (address != null ?
                                             (
                                                 " address = "
                                                 + address) : "")
                                          + (name != null ? (" name = " + name) : ""));

            // create the new router and get it's name
            routerAttrs = lci.newRouter(id, port, port + 1, address, name);

            BasicRouterInfo br = new BasicRouterInfo
                    ((Integer)routerAttrs.get(
                        "routerID"),
                    gc.getElapsedTime(), local,
                    (Integer)routerAttrs.get("mgmtPort"),
                    (Integer)routerAttrs.get("r2rPort"));
            br.setName((String)routerAttrs.get("name"));
            br.setAddress((String)routerAttrs.get("address"));

            // keep a handle on this router
            gc.addRouterInfo(id, br);
            Logger.getLogger("log").logln(USR.STDOUT, leadin()
                                          + "Created router " + routerAttrs);
            return true;
        } catch (JSONException e) {
            // Failed to start#
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin()
                                          + "Could not create router " + id
                                          + " on " + lci);

            if (port != 0) {
                pp.freePorts(port, port + 1);
            }

            // Free ports but different ones will be
            // tried next time
            return false;
        }
    }

    private static String leadin() {
        return "SRE:";
    }

}
