package usr.events;

import us.monoid.json.JSONObject;


/** Class represents an event*/
public interface Event {
    /** Accessor function for time*/
    public long getTime();

    /** Perform logic which preceeds an event */
    public void preceedEvent(EventDelegate obj);

    /** Execute the event and return a JSON object with information*/
    public JSONObject execute(EventDelegate obj) throws InstantiationException;

    /** Perform logic which follows an event */
    public void followEvent(JSONObject response, EventDelegate obj);

    /**
     * Get the event scheduler that processes this event
     */
    public EventScheduler getEventScheduler();

    /**
     * Set event scheduler  that will process this event
     */
    public void  setEventScheduler(EventScheduler es);
}
