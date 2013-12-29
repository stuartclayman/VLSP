package usr.events;

import us.monoid.json.JSONObject;


/**
 * An ExecutableEvent represents an Event that can be executed
 * by calling the execute() method.
 */
public interface ExecutableEvent extends Event {
    /** Execute the event and return a JSON object with information*/
    public JSONObject execute(EventDelegate obj) throws InstantiationException;

}

