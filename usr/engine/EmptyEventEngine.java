/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.StartSimulationEvent;
import usr.events.EndSimulationEvent;

public class EmptyEventEngine extends NullEventEngine {
    long timeToEnd_;

    /** Contructor from Parameter string */
    public EmptyEventEngine(int time, String parms) {
        timeToEnd_ = time * 1000;
        System.err.println("Empty engine ends "+time);
    }

    /** Initial events to add to schedule */
    @Override
    public void startStopEvents(EventScheduler s, EventDelegate g) {
        // simulation start
        StartSimulationEvent e0 = new StartSimulationEvent(0);

        s.addEvent(e0);

        // simulation end
        EndSimulationEvent e = new EndSimulationEvent(timeToEnd_);
        s.addEvent(e);
    }

    /** Initial events to add to schedule */
    @Override
    public void initialEvents(EventScheduler s, EventDelegate g) {
    }

    /** Add or remove events following a simulation event */
    @Override
    public void preceedEvent(Event e, EventScheduler s, EventDelegate g) {
    }

    /** Add or remove events following a simulation event */
    public void followEvent(Event e, EventScheduler s, EventDelegate g) {
    }

    @Override
    public void finalEvents(EventDelegate obj) {
    }

}
