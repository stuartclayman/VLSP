package usr.events;

import us.monoid.json.JSONObject;


/** 
 * The Event interface represents a generic event.
 * It has structure and a class to represent some event.
 * The receiver of an Event can do its own operations or
 * it can resolve a generic Event into an ExecutableEvent in order to
 * execute some operations.
 */
public interface Event {
    /** Accessor function for time*/
    public long getTime();

    /** Accessor function for parameters*/
    public String getParameters();
    
    /**
     * Get the event scheduler that processes this event
     */
    public EventScheduler getEventScheduler();

    /**
     * Set event scheduler  that will process this event
     */
    public void  setEventScheduler(EventScheduler es);


    /**
     * Get the context object that is used for this event
     */
    public Object getContextObject();

    /**
     * Set the context object that is used for this event
     */
    public void  setContextObject(Object co);


    /** Perform logic which preceeds an event */
    public void preceedEvent(EventDelegate obj);

    /** Perform logic which follows an event */
    public void followEvent(JSONObject response, EventDelegate obj);


}
