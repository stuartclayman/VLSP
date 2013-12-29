package usr.events;

import us.monoid.json.JSONObject;
import usr.engine.EventEngine;

/** Class represents a global controller event*/
public abstract class AbstractEvent implements Event {
    public final long time;
    protected EventScheduler scheduler_;
    public final EventEngine engine;


    /**
     * Important constructor to create Event.
     */
    protected AbstractEvent(long t, EventEngine eng) {
        // this sets the public final variables: time and engine
        time = t;
        engine = eng;
    }
        
    /** Return event as string*/
    @Override
    public abstract String toString();

    /** Accessor function for time*/
    @Override
    public long getTime() {
        return time;
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
        return scheduler_;
    }

    /**
     * Set event scheduler
     */
    public void  setEventScheduler(EventScheduler es) {
        scheduler_ = es;
    }

}
