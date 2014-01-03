package usr.events;

import us.monoid.json.JSONObject;


/**
 * An ExecutableEvent represents an Event that can be executed
 * by calling the execute() method.
 */
public interface ExecutableEvent extends Event {
    /** Execute the event, pass in a context object, and return a JSON object with information*/
    public JSONObject execute(EventDelegate obj, Object context);

    /**
     * The main body of the event.
     * This is called by execute.
     */
    public JSONObject eventBody(EventDelegate ed);

}

