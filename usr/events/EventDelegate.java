package usr.events;

import us.monoid.json.JSONObject;
import java.util.concurrent.TimeoutException;

/**
 * An event delegate has support for an EventScheduler.
 */
public interface EventDelegate {
    /** Execute an event,
     * return a JSON object with information about it
     * throws Instantiation if creation fails
     * Interrupted if acquisition of lock interrupted
     * Timeout if acquisition timesout
     */
    public JSONObject executeEvent(Event ev) throws InstantiationException, InterruptedException, TimeoutException;

    /**
     * checks if the delegate is active
     */
    public boolean isActive();

    /**
     * Deactivate the delegate
     */
    public void deactivate();

    /**
     * Get the maximum lag this delegate will allow.
     */
    public long getMaximumLag();

    /**
     * Get the name of the delegate
     */
    public String getName();

}
