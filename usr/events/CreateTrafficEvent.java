package usr.events;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.engine.BackgroundTrafficEngine;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class CreateTrafficEvent extends AbstractEvent {
    BackgroundTrafficEngine engine_;

    public CreateTrafficEvent(long time, EventEngine eng)
    throws InstantiationException {
        time_ = time;

        if (!(eng instanceof BackgroundTrafficEngine)) {
            throw new
                  InstantiationException
                  (
                      "Create Traffic Event requires BackgroundTrafficEngine");
        }

        engine_ = (BackgroundTrafficEngine)eng;
    }

    public String toString() {
        return new String("CreateTrafficEvent " + time_ + " ");
    }

    public JSONObject execute(GlobalController gc) throws
    InstantiationException {
        int nRouters = gc.getNoRouters();    // Cannot create traffic
        JSONObject json = new JSONObject();

        if (nRouters < 2) {
            try {
                json.put("success", false);
                json.put("msg", "Insufficient routers");
            } catch (JSONException e) {
            }

            return json;
        }

        int from;
        int to;

        if (engine_.preferEmptyNodes() ==
            true) {
            throw new InstantiationException(
                      "Not written engine to prefer empty nodes");
        }

        from = (int)Math.floor(Math.random() * nRouters);
        to = (int)Math.floor(Math.random() * (nRouters - 1));
        int toRouter = gc.getRouterId(to);
        int fromRouter = gc.getRouterId(from);

        if (to == from) {
            to = nRouters - 1;
        }

        String [] fromArgs = new String[4];
        String [] toArgs = new String[2];
        String port = ((Integer)engine_.getReceivePort(to)).toString();
        String bytes = ((Integer)engine_.getBytes()).toString();
        String rate = ((Double)engine_.getRate()).toString();
        BasicRouterInfo bri = gc.findRouterInfo(toRouter);
        fromArgs[0] = bri.getAddress();
        fromArgs[1] = port;
        toArgs[0] = port;
        fromArgs[2] = bytes;
        toArgs[1] = bytes;
        fromArgs[3] = rate;
        JSONObject start1 = gc.appStart(toRouter, "usr.applications.Receive", toArgs);
        JSONObject start2 = null;

        if (start1 != null) {
            start2 = gc.appStart(fromRouter, "usr.applications.Transfer", fromArgs);
        } else {
            // nothing to stop - it hasnt started yet
            // gc.appStop(start1);
        }

        try {
            if (start2 != null) {
                json.put("success", true);
                json.put("msg",
                         "traffic added between routers " + from + " " + to);
                json.put("router1", from);
                json.put("router2", to);
            } else {
                json.put("success", (Boolean)false);
                json.put("msg",
                         "Unable to create traffic between " + from + " " + to);
            }
        } catch (JSONException e) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "JSONException in CreateTrafficEvent should not occur");
        }

        return json;
    }

}
