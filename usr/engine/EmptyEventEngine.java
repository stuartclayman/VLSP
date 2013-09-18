/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import usr.events.EndSimulationEvent;
import usr.events.Event;
import usr.events.EventScheduler;
import usr.events.StartSimulationEvent;
import usr.globalcontroller.GlobalController;

public class EmptyEventEngine extends NullEventEngine {
    long timeToEnd_;

    /** Contructor from Parameter string */
    public EmptyEventEngine(int time, String parms) {
        timeToEnd_ = time * 1000;
    }

    /** Initial events to add to schedule */
    @Override
	public void startStopEvents(EventScheduler s, GlobalController g) {
        // simulation start
        StartSimulationEvent e0 = new StartSimulationEvent(0, this);

        s.addEvent(e0);

        // simulation end
        EndSimulationEvent e = new EndSimulationEvent(timeToEnd_, this);
        s.addEvent(e);
    }

    /** Initial events to add to schedule */
    @Override
	public void initialEvents(EventScheduler s, GlobalController g) {
    }

    /** Add or remove events following a simulation event */
    @Override
	public void preceedEvent(Event e, EventScheduler s, GlobalController g) {
    }

    /** Add or remove events following a simulation event */
    public void followEvent(Event e, EventScheduler s, GlobalController g) {
    }

}