package usr.events.globalcontroller;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.LinkInfo;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents an event which lists all links*/
public class ListLinksEvent extends AbstractGlobalControllerEvent {
    public ListLinksEvent(long time, EventEngine eng) {
        super(time, eng);
    }

    @Override
    public String toString() {
        String str = "ListLinks " + time + " ";

        return str;
    }

    @Override
    public JSONObject execute(GlobalController gc) {
        JSONObject jsobj = null;

        try {
            jsobj = new JSONObject();
            JSONArray array = new JSONArray();

            for (LinkInfo info : gc.getAllLinkInfo()) {
                array.put(info.getLinkID());
            }

            jsobj.put("type", "link");
            jsobj.put("list", array);
            return jsobj;
        } catch (JSONException js) {
            Logger.getLogger("log").logln(
                                          USR.ERROR,
                                          "JSONException in ListLinksEvent should not occur");
        }

        return jsobj;
    }

}
