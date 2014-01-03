package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndLinkEvent extends AbstractEvent {
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
