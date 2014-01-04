package usr.events.vimfunctions;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.engine.EventEngine;
import usr.logging.Logger;
import usr.logging.USR;
import usr.events.AbstractExecutableEvent;
import usr.events.EventDelegate;
import usr.events.vim.EndRouter;
import usr.vim.VimFunctions;
import us.monoid.json.JSONObject;


/** Class represents a global controller event*/
public class EndRouterEvent extends AbstractExecutableEvent implements EndRouter {
    public final int address;
    public final String name;

    public EndRouterEvent(long time, EventEngine eng, String addr) {
        super(time, eng);
        address = 0;
        name = addr;
    }

    public EndRouterEvent(long time, EventEngine eng, int rNo) {
        super(time, eng);
        address = rNo;
        name = null;
    }

    /**
     * Create a EndRouterEvent from an existing generic EndRouterEvent.
     */
    public EndRouterEvent(usr.events.vim.EndRouterEvent ere) {
        super(ere.time, ere.engine);

        if (ere.name == null) {  // name is null, so use address
            address = ere.address;
            name = null;
        } else {
            name = ere.name;
            address = 0;
        }
    }

    /** Execute the event, pass in a context object, and return a JSON object with information*/
    @Override
    public JSONObject eventBody(EventDelegate obj) {
        Object context = getContextObject();

        if (context instanceof VimFunctions) {
            try {
                VimFunctions vim = (VimFunctions)context;

                JSONObject routerData;

                if (name == null) {
                    // lookup router by number
                    routerData = vim.listRouters("address="+address);
                } else {
                    // lookup router by name
                    routerData = vim.listRouters("name="+name);

                }

                // get router id
                int routerID = routerData.getJSONArray("detail").getJSONObject(0).getInt("routerID");


                return vim.deleteRouter(routerID);

            } catch (JSONException jse) {
                return fail("JSONException " + jse.getMessage());
            }

        } else {
            return fail("Context object is not a VimFunctions");
        }
    }



    @Override
    public String toString() {
        String str;

        str = "EndRouter: " + time + " " + getName();
        return str;
    }

    private String getName() {
        String str = "";

        if (name != null) {
            str = name;
        }

        return str;
    }


}
