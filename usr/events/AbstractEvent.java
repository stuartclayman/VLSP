package usr.events;

import us.monoid.json.JSONObject;
import usr.engine.EventEngine;

/** Class represents a global controller event*/
public abstract class AbstractEvent implements Event {
    public final long time;
    public final String parameters;
    protected EventScheduler scheduler_;
    public final EventEngine engine;
    protected Object context;

    /**
     * Important constructor to create Event.
     */
    protected AbstractEvent(long t, EventEngine eng) {
        // this sets the public final variables: time and engine
        time = t;
        engine = eng;
        parameters = null;
    }
 
    /**
     * Important constructor to create Event (with parameters).
     */
    protected AbstractEvent(long t, EventEngine eng, String p) {
        // this sets the public final variables: time and engine
        time = t;
        engine = eng;
        parameters = p;
    }
        
    /** Return event as string*/
    @Override
    public abstract String toString();

    /** Accessor function for time*/
    @Override
    public long getTime() {
        return time;
    }
    
    /** Accessor function for parameters*/
    @Override
    public String getParameters() {
        return parameters;
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


    /**
     * Get the context object that is used for this event
     */
    public Object getContextObject() {
        return context;
    }

    /**
     * Set the context object that is used for this event
     */
    public void setContextObject(Object co) {
        context = co;
    }

    /** Perform logic which preceeds an event */
    @Override
    public  void preceedEvent(EventDelegate obj) {
        //System.out.println("AbstractEvent preceedEvent: " + this + " with engine " + engine);

        if (engine != null) {
            engine.preceedEvent(this, getEventScheduler(), obj);
        }

    }

    /** Perform logic which follows an event */
    @Override
    public  void followEvent(JSONObject response, EventDelegate obj) {
        //System.out.println("AbstractEvent followEvent: " + this + " with engine " + engine);

        if (engine != null) {
            engine.followEvent(this, getEventScheduler(), response, obj);
        }

    }


}
