package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event for creating a jvm in an app*/
public class StartJvmEvent extends AbstractEvent implements StartJvm {
    public final String className;
    public final String [] args;

    public StartJvmEvent(long time, EventEngine eng,  String cname, String [] args) {
        super(time, eng);
        className = cname;
        this.args = args;
    }


    @Override
    public String toString() {
        String str = "JvmStart " + time + getName();

        return str;
    }

    private String getName() {
        String str = " ";

        str +=  className + " Args:";

        for (String a : args) {
            str += " " + a;
        }

        return str;
    }

}
