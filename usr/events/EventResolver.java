package usr.events;

import us.monoid.json.JSONObject;


/**
 * An EventResolver is responsible for converting
 * a generic Event into an ExecutableEvent.
 */
public interface EventResolver {
    public ExecutableEvent resolveEvent(Event e);
}
