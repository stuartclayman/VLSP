package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartAppEvent extends AbstractEvent implements StartApp {
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

    public StartAppEvent(long time, EventEngine eng, String addr, String cname, String [] args) {
        super(time, eng);
        address = 0;
        className = cname;
        name = addr;
        this.args = args;
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
