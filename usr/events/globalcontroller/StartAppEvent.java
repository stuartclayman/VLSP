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
import usr.events.vim.StartApp;
import usr.common.BasicRouterInfo;
import usr.common.Pair;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.interactor.LocalControllerInteractor;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartAppEvent extends AbstractGlobalControllerEvent implements StartApp {
    int address_ = 0;
    String className_ = null;
    String [] args_ = null;
    String name_ = null;
    boolean routerNumSet_ = true;

    public StartAppEvent(long time, EventEngine eng, int rNo, String cname, String [] args) {
        super(time, eng);
        address_ = rNo;
        className_ = cname;
        args_ = args;
    }

    public StartAppEvent(long time, EventEngine eng, String name, String cname, String [] args) {
        super(time, eng);
        className_ = cname;
        name_ = name;
        args_ = args;
        routerNumSet_ = false;
    }

    public StartAppEvent(long time, EventEngine eng, String name, String cname, String [] args, GlobalController gc) throws InstantiationException {
        super(time, eng);
        className_ = cname;
        args_ = args;
        name_ = name;
        setRouterNo(name, gc);
    }

    /**
     * Create a StartAppEvent from an existing generic StartAppEvent
     */
    public StartAppEvent(usr.events.vim.StartAppEvent ase) {
        super(ase.time, ase.engine);

        if (ase.name == null) { // name is null, so use address
            address_ = ase.address;
            className_ = ase.className;
            args_ = ase.args;
            routerNumSet_ = true;
        } else {
            name_ = ase.name;
            className_ = ase.className;
            args_ = ase.args;
            routerNumSet_ = false;
        }
    }


    @Override
    public String toString() {
        String str = "AppStart " + time + getName();

        return str;
    }

    private String getName() {
        String str = " ";

        if (name_ == null) {
            str += (address_ + " ");
        } else {
            str += (name_ + " ");
        }

        str += className_ + " Args:";

        for (String a : args_) {
            str += " " + a;
        }

        return str;
    }

    private void setRouterNo(String name, GlobalController gc) throws InstantiationException {
        BasicRouterInfo rInfo = gc.findRouterInfo(name);

        if (rInfo == null) {
            throw new InstantiationException("Cannot find router " + name);
        }

        address_ = rInfo.getId();
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject jsobj = appStart(address_, className_, args_, gc);

        return jsobj;

    }


    protected JSONObject appStart(int routerID, String className, String[] args, GlobalController gc) {

        // register the app
        JSONObject json = new JSONObject();
        try {

            int appID;

            try {
                if (!routerNumSet_) {
                    setRouterNo(name_, gc);
                }

                // Try and start the app
                appID = appStartTry(address_, className_, args_, gc);
            } catch (Exception e) {
                appID = -1;
            }


            if (appID >= 0) {
                json.put("success", true);
                BasicRouterInfo bri = gc.findAppInfo(appID);
                String appName = bri.getAppName(appID);
                Map<String, Object> data = bri.getApplicationData(appName);
                json.put("id", appID);
                json.put("aid", data.get("aid"));
                json.put("name", appName);
                json.put("routerID", bri.getId());
                json.put("msg", "Started Application on router " + getName());
            } else {
                json.put("success", (Boolean)false);
                json.put("msg", "Unable to start application on router " + getName());
            }
        } catch (JSONException e) {
            Logger.getLogger("log").logln( USR.ERROR, "JSONException in AppStartEvent should not occur");
        }

        return json;
    }

    /**
     * Run an application on a Router.
     * Returns the app ID
     */
    protected int appStartTry(int routerID, String className, String[] args, GlobalController gc) {
        // return +ve no for valid id
        // return -1 for no start - cant find LocalController
        BasicRouterInfo br = gc.findRouterInfo(routerID);

        if (br == null) {
            System.err.println ("Router "+routerID+" does not exist when trying to start app");
            return -1;
        }

        LocalControllerInteractor lci = gc.getLocalController(br);

        if (lci == null) {
            System.err.println ("LocalControllerInteractor does not exisit when trying to start app on Router "+routerID);
            return -1;
        }

        int i;
        int MAX_TRIES = 5;
        Integer appID = -1;

        for (i = 0; i < MAX_TRIES; i++) {
            try {
                // appStart returns a JSONObject
                // something like: {"aid":1,"startTime":1340614768099,
                // "name":"/R4/App/usr.applications.RecvDataRate/1"}

                JSONObject response = lci.appStart(routerID, className, args);

                if (response.has("error")) {
                    // there was an error
                    // try again
                    continue;
                }

                // consturct an ID from the routerID and the appID
                Pair<Integer, Integer> idPair = new Pair<Integer, Integer>(routerID, (Integer)response.get("aid"));
                appID = idPair.hashCode();
                String appName = (String)response.get("name");

                // Add app to BasicRouterInfo
                br.addApplication(appID, appName);

                // and set info as
                // ["id": 46346535, "time" : "00:14:52", "aid" : 1,
                // "startime" : 1331119233159, "state": "RUNNING",
                // "classname" : "usr.applications.Send", "args" : "[4,
                // 3000, 250000, -d, 250, -i, 10]" ]
                Map<String, Object> dataMap = new HashMap<String, Object>();
                dataMap.put("time", "00:00:00");
                dataMap.put("id", appID);
                dataMap.put("aid", response.get("aid"));
                dataMap.put("startime", response.get("startTime"));
                dataMap.put("runtime", 0);
                dataMap.put("classname", className);
                dataMap.put("args", Arrays.asList(args).toString());
                dataMap.put("state", "STARTED");

                br.setApplicationData(appName, dataMap);

                // register app info
                gc.registerApp(time, appID, routerID);

                return appID;
            } catch (JSONException je) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " failed to start app " + className + " on "
                                              + routerID + " try " + i + " with Exception " + je);
            } catch (IOException io) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + " failed to start app " + className + " on "
                                              + routerID + " try " + i + " with Exception " + io);
            }
        }

        Logger.getLogger("log").logln(USR.ERROR, leadin() + " failed to start app " + className
                                      + " on " + routerID + " giving up ");
        return -1;
    }

    private String leadin() {
        return "GC(AppStartEv):";
    }


}
