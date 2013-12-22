package usr.events.globalcontroller;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.AbstractEvent;
import usr.engine.EventEngine;
import usr.globalcontroller.GlobalController;

/** Class represents a global controller event*/
public abstract class AbstractGlobalControllerEvent extends AbstractEvent implements Event {
    protected EventEngine engine_;

    /** Execute the event and return a JSON object with information*/
    @Override
    public  JSONObject execute(EventDelegate obj) throws InstantiationException {
        return execute((GlobalController)obj);
    }


    public abstract JSONObject execute(GlobalController gc) throws InstantiationException;

    /** Return event as string*/
    @Override
    public abstract String toString();

    /** Accessor function for time*/
    @Override
    public long getTime() {
        return time_;
    }

    /** Perform logic which follows an event */
    @Override
    public void followEvent(JSONObject response, EventDelegate obj) {
        followEvent(response, (GlobalController)obj);
    }

    public void followEvent(JSONObject response, GlobalController gc) {
        if (engine_ != null) {
            engine_.followEvent(this, getEventScheduler(), response, gc);
        }

        // Non-engine specific follows go here
    }

    /** Perform logic which preceeds an event */
    @Override
    public void preceedEvent(EventDelegate obj) {
        preceedEvent((GlobalController)obj);
    }

    public void preceedEvent(GlobalController gc) {

        if (engine_ != null) {
            engine_.preceedEvent(this, getEventScheduler(), gc);
        }

        // Non-engine specific preceeding actions go here
    }

}
