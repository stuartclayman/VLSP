package usr.events;

import us.monoid.json.JSONObject;
import usr.globalcontroller.GlobalController;


/** Class represents a global controller event*/
public interface Event {
    /** Execute the event and return a JSON object with information*/
    public  JSONObject execute(GlobalController gc) throws InstantiationException;

    /** Accessor function for time*/
    public long getTime();

    /** Perform logic which follows an event */
    public void followEvent(EventScheduler s, JSONObject response, GlobalController g);

    /** Perform logic which preceeds an event */
    public void preceedEvent(EventScheduler s, GlobalController g);
}
