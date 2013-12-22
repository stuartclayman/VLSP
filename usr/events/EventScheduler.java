package usr.events;

import java.util.ArrayList;


public interface EventScheduler extends Runnable {
    /**
     * Start the EventScheduler.
     */
    public boolean start();

    /**
     * Stop the EventScheduler.
     */
    public boolean stop();

    /** Return start time */
    public long getStartTime();

    /** Return the time since the start of the run
     * It is important to note that this can be called between events.
     */
    public long getElapsedTime();

    /** Get the time of the last event
     */
    public long getLastEventTime();

    /** Get the duration of the last event
     */
    public long getLastEventDuration();

    /** Adds an event to the schedule in time order
     */
    public void addEvent(Event e);

    /** Return first event from schedule
     */
    public Event getFirstEvent();

    /**
     * @return list of all scheduled events
     */
    public ArrayList <Event> getEvents();

}

