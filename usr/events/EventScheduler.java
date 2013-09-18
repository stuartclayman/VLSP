package usr.events;


public interface EventScheduler extends Runnable {

    /** Return the time since the start of the simulation*/
    public long getElapsedTime();

    /** Return start time */
    public long getStartTime();

    /** Get the current time into the simulation.
     * It is important to note that this can be called between events.
     */
    public long getSimulationTime();

    /** Adds an event to the schedule in time order
     */
    public void addEvent(Event e);

    /** Return first event from schedule
     */
    public Event getFirstEvent();

    /** Interrupt above wait*/
    public void wakeWait();

}
