package usr.events.vimfunctions;

import usr.engine.EventEngine;
import usr.logging.Logger;
import usr.logging.USR;
import usr.events.AbstractExecutableEvent;
import usr.events.vim.EndLink;
import usr.events.EventDelegate;
import usr.vim.VimFunctions;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;

/** Class represents a global controller event*/
public class EndLinkEvent extends AbstractExecutableEvent implements EndLink {
    public final int address1;
    public final int address2;
    public final String name1;
    public final String name2;

    public EndLinkEvent(long time, EventEngine eng, int r1, int r2) {
        super(time, eng);
        address1 = r1;
        address2 = r2;
        name1 = null;
        name2 = null;
    }

    public EndLinkEvent(long time, EventEngine eng, String add1, String add2) {
        super(time, eng);
        address1 = 0;
        address2 = 0;
        name1 = add1;
        name2 = add2;
    }


    /**
     * Create a EndLinkEvent from an existing generic EndLinkEvent
     */
    public EndLinkEvent(usr.events.vim.EndLinkEvent ele) {
        super(ele.time, ele.engine);

        if (ele.name1 == null) {   // name is null, so use address
            address1 = ele.address1;
            address2 = ele.address2;
            name1 = null;
            name2 = null;
        } else {
            name1 = ele.name1;
            name2 = ele.name2;
            address1 = 0;
            address2 = 0;
        }
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

                // now we have IDs for both routers
                // so get all the links out of router1
                JSONArray linksOut = router1Data.getJSONArray("detail").getJSONObject(0).getJSONArray("links");
                JSONArray linkIDs = router1Data.getJSONArray("detail").getJSONObject(0).getJSONArray("linkIDs");
                // vim.listRouterLinks(router1ID, "connected");

                System.out.println("links for router1 = " + linksOut + " / " + linkIDs);

                
                // now detemrine linkID
                int linkID = 0;
                for (int l=0; l<linksOut.length(); l++) {
                    int dstRouter = linksOut.getInt(l);
                    int link = linkIDs.getInt(l);

                    if (dstRouter == router2ID) { // we found it
                        linkID = link;
                        break;
                    }
                }
                    

                return vim.deleteLink(linkID);
            } catch (JSONException jse) {
                return fail("JSONException " + jse.getMessage());
            }

        } else {
            return fail("Context object is not a VimFunctions");
        }
    }

    @Override
    public String toString() {
        String str = "EndLinkEvent " + time + getName();

        return str;
    }

    private String getName() {
        String str = " ";

        if (name1 == null) {
            str += address1 + " " + address2;
        } else {
            str += name1 + " " + name2;
        }

        return str;
    }


}
