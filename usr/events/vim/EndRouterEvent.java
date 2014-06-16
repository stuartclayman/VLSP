package usr.events.vim;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndRouterEvent extends AbstractEvent implements EndRouter {
    public final int address;
    public final String name;

    public EndRouterEvent(long time, EventEngine eng, String nm) {
        super(time, eng);
        address = 0;
        name = nm;
    }

    public EndRouterEvent(long time, EventEngine eng, int rNo) {
        super(time, eng);
        address = rNo;
        name = null;
    }

    @Override
    public String toString() {
        String str;

        str = "EndRouter: " + time + " " + getName();
        return str;
    }
    
    public int getRouterNumber() {
		return address;
	}
    
    private String getName() {
        String str = "";

        if (name != null) {
            str = name;
        }

        return str;
    }


}
