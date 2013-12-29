package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class AppStartEvent extends AbstractEvent {
    public final int routerNo;
    public final String className;
    public final String [] args;
    public final String address;

    public AppStartEvent(long time, EventEngine eng, int rNo, String cname, String [] args) {
        super(time, eng);
        routerNo = rNo;
        address = null;
        className = cname;
        this.args = args;
    }

    public AppStartEvent(long time, EventEngine eng, String addr, String cname, String [] args) {
        super(time, eng);
        routerNo = 0;
        className = cname;
        address = addr;
        this.args = args;
    }

    @Override
    public String toString() {
        String str = "AppStart " + time + getName();

        return str;
    }

    private String getName() {
        String str = "";

        if (address == null) {
            str += (routerNo + " ");
        } else {
            str += (address + " ");
        }

        str += className + " Args:";

        for (String a : args) {
            str += " " + a;
        }

        return str;
    }

}
