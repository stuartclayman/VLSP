package usr.events.globalcontroller;

import us.monoid.json.JSONObject;
import usr.events.ExecutableEvent;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.AbstractExecutableEvent;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;

/** Class represents a global controller event*/
public abstract class AbstractGlobalControllerEvent extends AbstractExecutableEvent implements ExecutableEvent {
    /**
     * Important constructor to create Event.
     */
    protected AbstractGlobalControllerEvent(long t, EventEngine eng) {
        super(t, eng);
    }
        
    /** Execute the event and return a JSON object with information*/
    @Override
    public  JSONObject eventBody(EventDelegate obj) {
        return execute((GlobalController)getContextObject());
    }


    public abstract JSONObject execute(GlobalController gc);

    /** Return event as string*/
    @Override
    public abstract String toString();

    /** Perform logic which follows an event */
    @Override
    public void followEvent(JSONObject response, EventDelegate obj) {
        super.followEvent(response, obj);

        // Non-engine specific follows go here
        if (obj instanceof GlobalController) {
            followEvent(response, (GlobalController)obj);
        }
                        
    }

    public void followEvent(JSONObject response, GlobalController gc) {
    }

    /** Perform logic which preceeds an event */
    @Override
    public void preceedEvent(EventDelegate obj) {
        super.preceedEvent(obj);

        // Non-engine specific preceeding actions go here
        if (obj instanceof GlobalController) {
            preceedEvent((GlobalController)obj);
        }
                        
    }

    public void preceedEvent(GlobalController gc) {
    }

}
