package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class EndLinkEvent extends AbstractEvent {
    public final int router1;
    public final int router2;
    public final String address1;
    public final String address2;

    public EndLinkEvent(long time, EventEngine eng, int r1, int r2) {
        super(time, eng);
        router1 = r1;
        router2 = r2;
        address1 = null;
        address2 = null;
    }

    public EndLinkEvent(long time, EventEngine eng, String add1, String add2) {
        super(time, eng);
        router1 = 0;
        router2 = 0;
        address1 = add1;
        address2 = add2;
    }


    @Override
    public String toString() {
        String str = "EndLinkEvent " + time + getName();

        return str;
    }

    private String getName() {
        String str = "";

        if (address1 == null) {
            str += router1 + " " + router2;
        } else {
            str += address1 + " " + address2;
        }

        return str;
    }


}
