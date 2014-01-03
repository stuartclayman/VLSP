package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.engine.BackgroundTrafficEngine;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class CreateTrafficEvent extends AbstractGlobalControllerEvent {
    BackgroundTrafficEngine engine_;

    public CreateTrafficEvent(long time, EventEngine eng) throws InstantiationException {
        super(time, eng);

        if (!(eng instanceof BackgroundTrafficEngine)) {
            throw new InstantiationException ("Create Traffic Event requires BackgroundTrafficEngine");
        }

        engine_ = (BackgroundTrafficEngine)eng;
    }

    @Override
    public String toString() {
        return new String("CreateTrafficEvent " + time + " ");
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        int nRouters = gc.getNoRouters();    
        JSONObject json = new JSONObject();

        if (nRouters < 2) {      // Cannot create traffic
            return fail("Insufficient routers");
        }

        int from;
        int to;

        if (engine_.preferEmptyNodes() == true) {
            return fail("Not written engine to prefer empty nodes");
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
        JSONObject start1 = null;
        JSONObject start2 = null;

        try {
            start1 = gc.createApp(toRouter, "usr.applications.Receive", toArgs);

            if (start1 != null) {
                start2 = gc.createApp(fromRouter, "usr.applications.Transfer", fromArgs);
            } else {
                // nothing to stop - it hasnt started yet
                // gc.appStop(start1);
            }


            if (start2 != null) {
                json.put("success", true);
                json.put("msg", "traffic added between routers " + from + " " + to);
                json.put("router1", from);
                json.put("router2", to);
            } else {
                json.put("success", (Boolean)false);
                json.put("msg", "Unable to create traffic between " + from + " " + to);
            }
        } catch (JSONException e) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in CreateTrafficEvent should not occur");
        }

        return json;
    }


}
