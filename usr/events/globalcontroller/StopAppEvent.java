package usr.events.globalcontroller;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.vim.StopApp;
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
public class StopAppEvent extends AbstractGlobalControllerEvent implements StopApp {
    int routerNo_ = 0;
    int appID = 0;

    public StopAppEvent(long time, EventEngine eng, int rNo, int appNo) {
        super(time, eng);
        routerNo_ = rNo;
        appID = appNo;
    }

    @Override
	public String toString() {
        String str = "AppStop " + time + getName();

        return str;
    }

    private String getName() {
        String str = "";

        str += (routerNo_ + " " + appID);

        return str;
    }


    @Override
    public JSONObject execute(GlobalController gc) {

        JSONObject jsobj = appStop(routerNo_, appID, gc);

        return jsobj;

    }


    /**
     * Stop an application on a Router.
     * Takes the router ID and the appID
     * Returns int
     */
    protected JSONObject appStop(int routerID, int appID, GlobalController gc) {
        // Try and stop the app
        int result = appStopTry(routerNo_, appID, gc);

        JSONObject json = new JSONObject();
        
        try {
            if (result >=0) {
                json.put("success", true);
            } else {
                json.put("success", false);
                json.put("msg", "Unable to stop application on router " + getName());
            }
        } catch (JSONException e) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException in AppStopEvent should not occur");
        }


        return json;

    }

    protected int appStopTry(int routerID, int appID, GlobalController gc) {
        BasicRouterInfo br = gc.findRouterInfo(routerID);

        LocalControllerInteractor lci =  gc.getLocalController(br);

        if (lci == null) {
            return -1;
        }
        int i;
        int MAX_TRIES = 5;

        String appName = br.getAppName(appID);

        System.out.println("AppID: " + appID + " -> " + "AppName: " + appName);

        //Map<String, Object> data = br.getApplicationData(appName);


        for (i = 0; i < MAX_TRIES; i++) {
            try {
                // appStop returns a JSONObject

                JSONObject response = lci.appStop(routerID, appName);


                // and unset info as

                // Add app to BasicRouterInfo
                br.removeApplication(appID, appName);

                // remove app to app info
                gc.unregisterApp(time, appID);

                return appID;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin()+
                                              " failed to stop app "+appName+" on "+ routerID+ " try "+i + " with Exception " +
                                              e);

            }
        }
        Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                      " failed to start app "+appName+" on "+ routerID+ " giving up ");
        return -2;
    }


    private String leadin() {
        return "GC(AppStopEv):";
    }


}
