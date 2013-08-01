package usr.events;

import us.monoid.json.JSONObject;
import usr.globalcontroller.GlobalController;

public interface EventScheduler extends Runnable {

    /** Return the time since the start of the simulation*/
    public long getElapsedTime();

    /** Return start time */
    public long getStartTime();

    /** Adds an event to the schedule in time order
     */
    public void addEvent(Event e);

    /** Return first event from schedule
     */
    public Event getFirstEvent();

    /** Interrupt above wait*/
    public void wakeWait();

}
