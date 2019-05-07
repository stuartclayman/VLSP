package usr.events.globalcontroller;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.vim.StartJvm;
import usr.common.BasicJvmInfo;
import usr.common.Pair;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartJvmEvent extends AbstractGlobalControllerEvent implements StartJvm {
    String className_ = null;
    String [] args_ = null;



    public StartJvmEvent(long time, EventEngine eng, String cname, String [] args) {
        super(time, eng);
        className_ = cname;
        args_ = args;
    }

    /**
     * Create a StartJvmEvent from an existing generic StartJvmEvent
     */
    public StartJvmEvent(usr.events.vim.StartJvmEvent ase) {
        super(ase.time, ase.engine);

        className_ = ase.className;
        args_ = ase.args;
    }


    @Override
    public String toString() {
        String str = "JvmStart " + time + getName();

        return str;
    }

    private String getName() {
        String str = " ";

        str += className_ + " Args:";

        for (String a : args_) {
            str += " " + a;
        }

        return str;
    }


    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject jsobj = startJvm(gc, time, className_, args_);

        try {
            if (jsobj == null) {
                jsobj.put("success", false);
                jsobj.put("msg", "Could not create jvm");
            } else {
                int rNo = (Integer)jsobj.get("jvmID");
                jsobj.put("success", true);
                jsobj.put("jvmID", rNo);

                String status = (String)jsobj.get("status");
                
                if (status.equals("completed")) {
                    jsobj.put("msg", "Completed JVM " + rNo + " " + getName());
                } else {
                    BasicJvmInfo bri = gc.findJvmInfo(rNo);
                    jsobj.put("name", bri.getName());


                    jsobj.put("msg", "Created JVM " + rNo + " " + getName());
                }
            }
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in StartJvmEvent should not occur " + je.getMessage());
        }

        return jsobj;
    }

    public JSONObject startJvm(GlobalController gc, long time, String className, String [] args) {
        JSONObject startVal;

        int jId = gc.getNextJvmId();
        if ((startVal = doJvmStart(gc, jId, className, args)) == null) {
            return null;
        }

        gc.registerJvm(time, jId);

        return startVal;
    }

    private JSONObject doJvmStart(GlobalController gc, int jid, String className, String [] args) {
        // Find least used local controller (either by passing extra parameters or not)
	
	Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Choosing LocalControllerInfo with extra parameters " + parameters);

        LocalControllerInfo leastUsed = gc.placementForRouter(className, null);

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Choose LocalControllerInfo " + leastUsed);

        if (leastUsed == null) {
            return null;
        }

        LocalControllerInteractor lci = gc.getLocalController(leastUsed);
        int MAX_TRIES = 5;

        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                JSONObject retVal;
                
                if ((retVal = tryJvmStart(gc, jid, className, args, leastUsed, lci)) != null) {
                    //System.err.println("Started");
                    leastUsed.addRouter(jid); // Increment count
                    
                    return retVal;
                }
            } catch (IOException e) {
                //e.printStackTrace();
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin() + "Could not start new router on "
                                              + leastUsed + " out of ports ");

                //System.err.println("Out of ports");
                return null;
            }
        }

        Logger.getLogger("log").logln(USR.ERROR,
                                      leadin() + "Could not start new router on "
                                      + leastUsed + " after " + MAX_TRIES + " tries.");

        //System.err.println("Could not start");
        return null;
    }

    /** Make one attempt to start a router */
    private JSONObject tryJvmStart(GlobalController gc, int jid, String className, String [] args, LocalControllerInfo local, LocalControllerInteractor lci) throws IOException {
        int port = 0;

        JSONObject jvmAttrs;

        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()
                                          + "Creating jvm: " + jid + " " + className + " " + Arrays.asList(args));

            // create the new jvm and get it's name
            jvmAttrs = lci.newJvm(jid, className, args);

            String status  = (String)jvmAttrs.get("status");

            if (status.equals("completed")) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Created jvm " + jvmAttrs + " completed");
            } else if (status.equals("running")) {

                BasicJvmInfo br = new BasicJvmInfo ((Integer)jvmAttrs.get("jvmID"),
                                                    gc.getElapsedTime(), local,
                                                    className, args);

                // keep a handle on this jvm
                gc.addJvmInfo(jid, br);
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Created jvm " + jvmAttrs);
            } else {
            }
            
            return jvmAttrs;

        } catch (JSONException e) {
            // Failed to start#
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Could not create jvm " + jid
                                          + " on " + lci);

            return null;
        }
    }

    private String leadin() {
        return "GC(JvmStartEv):";
    }


}
