package usr.events.vimfunctions;

import usr.engine.EventEngine;
import usr.logging.Logger;
import usr.logging.USR;
import usr.events.AbstractExecutableEvent;
import usr.events.EventDelegate;
import usr.vim.VimFunctions;
import usr.events.vim.StartApp;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONException;

/** Class represents a global controller event*/
public class StartAppEvent extends AbstractExecutableEvent implements StartApp {
    public final int address;
    public final String className;
    public final String [] args;
    public final String name;

    public StartAppEvent(long time, EventEngine eng, int rNo, String cname, String [] args) {
        super(time, eng);
        address = rNo;
        name = null;
        className = cname;
        this.args = args;
    }

    public StartAppEvent(long time, EventEngine eng, String name, String cname, String [] args) {
        super(time, eng);
        address = 0;
        className = cname;
        this.name = name;
        this.args = args;
    }

    /**
     * Create a StartAppEvent from an existing generic StartAppEvent
     */
    public StartAppEvent(usr.events.vim.StartAppEvent ase) {
        super(ase.time, ase.engine);

        if (ase.name == null) { // name is null, so use address
            address = ase.address;
            name = null;
            className = ase.className;
            args = ase.args;
        } else {
            address = 0;
            name = ase.name;
            className = ase.className;
            args = ase.args;
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

                StringBuilder builder = new StringBuilder();
                for (String arg : args) {
                    builder.append(arg);
                    builder.append(" ");
                }


                return vim.createApp(routerID, className, builder.toString());
            } catch (JSONException jse) {
                return fail("JSONException " + jse.getMessage());
            }


        } else {
            return fail("Context object is not a VimFunctions");
        }
    }



    @Override
    public String toString() {
        String str = "AppStart " + time + getName();

        return str;
    }

    private String getName() {
        String str = " ";

        if (name == null) {
            str += (address + " ");
        } else {
            str += (name + " ");
        }

        str +=  className + " Args:";

        for (String a : args) {
            str += " " + a;
        }

        return str;
    }

}
