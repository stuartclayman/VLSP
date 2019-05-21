package demo_usr.nfv;

import us.monoid.json.JSONObject;
import usr.applications.ApplicationResponse;

import demo_usr.paths.Reconfigure;
import demo_usr.paths.ManagementHandler;

/**
 * A class that handles a callback from a ManagementListener
 */
public class ReconfigureHandler implements ManagementHandler {
    Reconfigure reconfigure = null;

    /**
     * Construct a ReconfigureHandler with a reference
     * to the object that is to be reconfigured.
     */
    public ReconfigureHandler(Reconfigure r) {
        this.reconfigure = r;
    }

    /**
     * Simply calls directly to the Reconfigure callback.
     */
    public Object process(JSONObject jsobj) {
        Object result =  reconfigure.process(jsobj);

        return result;
    }
}
