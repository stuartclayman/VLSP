package usr.events.vim;

import usr.engine.EventEngine;
import usr.events.AbstractEvent;
import usr.logging.Logger;
import usr.logging.USR;

/** Class represents a global controller event*/
public class StartRouterEvent extends AbstractEvent implements StartRouter {
    public final String address;
    public final String name;
    public final String parameters; // added optional parameters for service load prediction (Lefteris)

    public StartRouterEvent(long time, EventEngine eng) {
        super(time, eng);
        this.name = null;
        this.address = null;
        this.parameters =null;
    }

    public StartRouterEvent(long time, String parameters, EventEngine eng) {
        super(time, eng, parameters);
        this.name = null;
        this.address = null;
        this.parameters = parameters;
    }
    
    public StartRouterEvent(long time, EventEngine eng, String address, String name) {
        super(time, eng);
        this.name = name;
        this.address = address;
        this.parameters=null;
    }

    public StartRouterEvent(long time, EventEngine eng, String address, String name, String parameters) {
        super(time, eng, parameters);
        this.name = name;
        this.address = address;
        this.parameters = parameters;
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
