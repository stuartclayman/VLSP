package usr.events.globalcontroller;

import java.io.IOException;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.vim.StopJvm;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicJvmInfo;
import usr.common.PortPool;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.model.lifeEstimate.LifetimeEstimate;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.Logger;
import usr.logging.USR;


/** Class represents a global controller event*/
public class StopJvmEvent extends AbstractGlobalControllerEvent implements StopJvm {
    int jvmID = 0;

    public StopJvmEvent(long time, EventEngine eng, int jvmNo) {
        super(time, eng);
        jvmID = jvmNo;
    }

    @Override
    public String toString() {
        String str = "JvmStop " + time + getName();

        return str;
    }

    private String getName() {
        String str = "";

        str += (jvmID);

        return str;
    }

    @Override
    public JSONObject execute(GlobalController gc) {

        JSONObject json = new JSONObject();
        try {


            boolean success = endJvm(jvmID, gc, time);

            if (success) {
                json.put("success", (Boolean)true);
                json.put("msg", "Shut down jvm " + jvmID + " " + getName());
                json.put("jvm", jvmID);

            } else {
                json.put("success", (Boolean)false);
                json.put("msg", "Could not shut down jvm " + getName());
            }
        } catch (JSONException js) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in StopJvmEvent should not occur");
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Exception in StopJvmEvent should not occur");
            e.printStackTrace();
        }

        return json;
    }

    /** Event to end a jvm -- returns true for success */
    public boolean endJvm(int jvmId, GlobalController gc, long time) {
        boolean success;

        success = endJvm(jvmId, gc);

        if (success) {
            gc.unregisterJvm(time, jvmId);
        }

        return success;
    }

    /** Send shutdown to an emulated jvm */
    private boolean endJvm(int jid, GlobalController gc) {
        BasicJvmInfo bji = gc.findJvmInfo(jid);

        if (bji == null) {
            return false;
        }

        LocalControllerInteractor lci = gc.getLocalController(bji);

        int MAX_TRIES = 5;
        int i = 0;
        for (i = 0; i < MAX_TRIES; i++) {
            try {
                JSONObject jsobj = lci.stopJvm(jid);
                break;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()
                                              + "Cannot shut down jvm " + 
                                              + jid + " attempt "
                                              + (i + 1) + " Exception = " + e);
            }
        }

        if (i == MAX_TRIES) {
            return false;
        }

        gc.removeJvmInfo(jid);
        
        return true;

    }


    private String leadin() {
        return "GC(JvmStopEv):";
    }


}
