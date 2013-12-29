package usr.events.vim;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndRouterEvent extends AbstractEvent {
    public final int routerNo;
    public final String address;

    public EndRouterEvent(long time, EventEngine eng, String addr) {
        super(time, eng);
        routerNo = 0;
        address = addr;
    }

    public EndRouterEvent(long time, EventEngine eng, int rNo) {
        super(time, eng);
        routerNo = rNo;
        address = null;
    }

    @Override
    public String toString() {
        String str;

        str = "EndRouter: " + time + " " + getName();
        return str;
    }

    private String getName() {
        String str = "";

        if (address != null) {
            str = address;
        }

        return str;
    }


}
