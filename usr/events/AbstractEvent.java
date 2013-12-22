package usr.events;

import us.monoid.json.JSONObject;

/** Class represents a global controller event*/
public abstract class AbstractEvent implements Event {
    protected long time_;
    protected EventScheduler scheduler;

    /** Execute the event and return a JSON object with information*/
    @Override
    public abstract JSONObject execute(EventDelegate obj) throws InstantiationException;

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
    public  void followEvent(JSONObject response, EventDelegate obj) {
    }

    /** Perform logic which preceeds an event */
    @Override
    public  void preceedEvent(EventDelegate obj) {
    }

    /**
     * Get event scheduler
     */
    public EventScheduler getEventScheduler() {
        return scheduler;
    }

    /**
     * Set event scheduler
     */
    public void  setEventScheduler(EventScheduler es) {
        scheduler = es;
    }

}
