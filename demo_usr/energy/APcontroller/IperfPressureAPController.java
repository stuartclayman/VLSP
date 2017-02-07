package demo_usr.energy.APcontroller;

import usr.APcontroller.PressureAPController;
import usr.router.RouterOptions;
import usr.globalcontroller.GlobalController;
import usr.common.BasicRouterInfo;
import usr.localcontroller.LocalControllerInfo;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class IperfPressureAPController extends PressureAPController {

    public IperfPressureAPController(RouterOptions o) {
        super(o);
    }

    /** Get some context data **/
    public String[] getContextData(long time, int gid, int ap, int cost, GlobalController g) {
        // get BasicRouterInfo for gid
        BasicRouterInfo gidBR = g.findRouterInfo(gid);
        LocalControllerInfo gidLCI = gidBR.getLocalControllerInfo();

        // get BasicRouterInfo for ap
        BasicRouterInfo apBR = g.findRouterInfo(ap);
        LocalControllerInfo apLCI = apBR.getLocalControllerInfo();

        // Allocate a String to hold a JSON string
        String[] result = new String[1];

        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("gid", gid);
            jsobj.put("gidIP", gidBR.getLocalControllerInfo().getIp().getHostAddress());
            jsobj.put("ap", ap);
            jsobj.put("apIP", apBR.getLocalControllerInfo().getIp().getHostAddress());
        } catch (JSONException je) {
        }

        result[0] = jsobj.toString();
        
        //System.err.println("IperfPressureAPController: getContextData = " + result[0]);

        return  result;
    }

  

}
