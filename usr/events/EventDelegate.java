package usr.events;

import us.monoid.json.JSONObject;
import java.util.concurrent.TimeoutException;
import usr.output.OutputType;

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
    public JSONObject executeEvent(Event ev) throws Exception;

    /**
     * checks if the delegate is active
     */
    public boolean isActive();

    /**
     * Notification for start of EventScheduler
     */
    public void onEventSchedulerStart(long time);

    /**
     * Notification for stop of EventScheduler
     */
    public void onEventSchedulerStop(long time);

    /** 
     * Notification for an event execution success 
     */
    public void onEventSuccess(long time, Event ev);

    /**
     * Notification for an event execution failure
     */
    public void onEventFailure(long time, Event ev);

    /**
     * Get the maximum lag this delegate will allow.
     */
    public long getMaximumLag();

    /**
     * Get the name of the delegate
     */
    public String getName();

}
