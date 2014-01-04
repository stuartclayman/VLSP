package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartRouterEvent extends AbstractEvent implements StartRouter {
    public final String address;
    public final String name;

    public StartRouterEvent(long time, EventEngine eng) {
        super(time, eng);
        this.name = null;
        this.address = null;
    }

    public StartRouterEvent(long time, EventEngine eng, String address, String name) {
        super(time, eng);
        this.name = name;
        this.address = address;
    }

    @Override
    public String toString() {
        String str;

        str = "StartRouter: " + time + " " + getName();
        return str;
    }

    private String getName() {
        String str = "";

        if (name != null) {
            str += " " + name;
        }

        if (address != null) {
            str += " " + address;
        }

        return str;
    }


}
