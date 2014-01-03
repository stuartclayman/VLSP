package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartLinkEvent extends AbstractEvent {
    public final int address1;
    public final int address2;
    public final String name1;
    public final String name2;
    private int weight = 1;
    private String linkName = null;

    public StartLinkEvent(long time, EventEngine eng, int r1, int r2) {
        super(time, eng);
        address1 = r1;
        address2 = r2;
        name1 = null;
        name2 = null;
    }

    public StartLinkEvent(long time, EventEngine eng, int r1, int r2, int w) {
        super(time, eng);
        address1 = r1;
        address2 = r2;
        name1 = null;
        name2 = null;
        weight = w;
    }

    public StartLinkEvent(long time, EventEngine eng, String add1, String add2) {
        super(time, eng);
        address1 = 0;
        address2 = 0;
        name1 = add1;
        name2 = add2;
    }

    public StartLinkEvent(long time, EventEngine eng, String add1, String add2, int w) {
        super(time, eng);
        address1 = 0;
        address2 = 0;
        name1 = add1;
        name2 = add2;
        weight = w;
    }

    public void setWeight(int w) {
        weight = w;
    }

    public int getWeight() {
        return weight;
    }

    public void setLinkName(String s) {
        linkName = s;
    }

    public String getLinkName() {
        return linkName;
    }

    @Override
    public String toString() {
        String str = "StartLink " + time + " " + getName();

        return str;
    }

    private String getName() {
        String str = "";

        if (name1 == null) {
            str += address1 + " " + address2 + " ";
        }

        if (name1 != null) {
            str += name1 + " " + name2;
        }

        return str;
    }

}
