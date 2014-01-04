package usr.events.vimfunctions;

import usr.engine.EventEngine;
import usr.logging.Logger;
import usr.logging.USR;
import usr.events.AbstractExecutableEvent;
import usr.events.EventDelegate;
import usr.vim.VimFunctions;
import usr.events.vim.StartLink;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONException;

/** Class represents a global controller event*/
public class StartLinkEvent extends AbstractExecutableEvent implements StartLink {
    public final int address1;
    public final int address2;
    public final String name1;
    public final String name2;
    private int weight = 1;
    private String linkName = null;

    public StartLinkEvent(long time, EventEngine eng, int r1, int r2) {
        super(time, eng);
        address1 = r1;
        address2 = r2;
        name1 = null;
        name2 = null;
    }

    public StartLinkEvent(long time, EventEngine eng, int r1, int r2, int w) {
        super(time, eng);
        address1 = r1;
        address2 = r2;
        name1 = null;
        name2 = null;
        weight = w;
    }

    public StartLinkEvent(long time, EventEngine eng, String add1, String add2) {
        super(time, eng);
        address1 = 0;
        address2 = 0;
        name1 = add1;
        name2 = add2;
    }

    public StartLinkEvent(long time, EventEngine eng, String add1, String add2, int w) {
        super(time, eng);
        address1 = 0;
        address2 = 0;
        name1 = add1;
        name2 = add2;
        weight = w;
    }

    /**
     * Create a StartLinkEvent from an existing generic StartLinkEvent
     */
    public StartLinkEvent(usr.events.vim.StartLinkEvent sle) {
        super(sle.time, sle.engine);

        if (sle.name1 == null) {   // address is null, so use routerNo
            address1 = sle.address1;
            address2 = sle.address2;
            name1 = null;
            name2 = null;
        } else {
            name1 = sle.name1;
            name2 = sle.name2;
            address1 = 0;
            address2 = 0;
        }


        weight =  sle.getWeight();
        linkName = sle.getLinkName();

    }



    /** Execute the event, pass in a context object, and return a JSON object with information*/
    @Override
    public JSONObject eventBody(EventDelegate obj) {
        Object context = getContextObject();

        if (context instanceof VimFunctions) {
            try {
                VimFunctions vim = (VimFunctions)context;

                JSONObject router1Data;
                JSONObject router2Data;

                if (name1 == null) { // names null, so use addresses
                    router1Data = vim.listRouters("address="+address1);
                    router2Data = vim.listRouters("address="+address2);
                } else { // names not null, so use names
                    router1Data = vim.listRouters("name="+name1);
                    router2Data = vim.listRouters("name="+name2);
                }

                //System.out.println("router1Data = " + router1Data);
                //System.out.println("router2Data = " + router2Data);

                // get router1 id
                int router1ID = router1Data.getJSONArray("detail").getJSONObject(0).getInt("routerID");
                // get router2 id
                int router2ID = router2Data.getJSONArray("detail").getJSONObject(0).getInt("routerID");

                JSONObject jsobj;

                if (linkName != null) {
                    jsobj = vim.createLink(router1ID, router2ID, weight, linkName);
                } else {
                    jsobj = vim.createLink(router1ID, router2ID, weight);
                }

                return jsobj;

            } catch (JSONException jse) {
                return fail("JSONException " + jse.getMessage());
            }
        } else {
            return fail("Context object is not a VimFunctions");
        }
    }


    public void setWeight(int w) {
        weight = w;
    }

    public int getWeight() {
        return weight;
    }

    public void setLinkName(String s) {
        linkName = s;
    }

    public String getLinkName() {
        return linkName;
    }

    @Override
    public String toString() {
        String str = "StartLink " + time + " " + getName();

        return str;
    }

    private String getName() {
        String str = "";

        if (name1 == null) {
            str += address1 + " " + address2 + " ";
        }

        if (name1 != null) {
            str += name1 + " " + name2;
        }

        return str;
    }

}
