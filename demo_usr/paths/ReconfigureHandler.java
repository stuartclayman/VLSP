package demo_usr.paths;

import us.monoid.json.JSONObject;
import usr.applications.ApplicationResponse;

public class ReconfigureHandler implements ManagementHandler {
    Reconfigure reconfigure = null;

    /**
     * Construct a ReconfigureHandler with a reference
     * to the object that is to be reconfigured.
     */
    public ReconfigureHandler(Reconfigure r) {
        this.reconfigure = r;
    }
    @Override
	public Object process(JSONObject jsobj) {
        Object result =  reconfigure.process(jsobj);

        if (result instanceof ApplicationResponse) {
            ApplicationResponse resp = (ApplicationResponse)result;

            if (resp.isSuccess()) {
                return result;
            } else {
                // Failed
                throw new Error(resp.getMessage());
            }
        } else {
            throw new Error("Reconfigure Failed");
        }
    }
}
