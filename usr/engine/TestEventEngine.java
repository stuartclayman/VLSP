/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import us.monoid.json.JSONObject;
import usr.events.EndSimulationEvent;
import usr.events.Event;
import usr.events.EventScheduler;
import usr.events.StartRouterEvent;
import usr.events.StartSimulationEvent;
import usr.globalcontroller.GlobalController;

public class TestEventEngine implements EventEngine {
    int timeToEnd_;

    /** Contructor from Parameter string */
    public TestEventEngine(int time, String parms) {
        timeToEnd_ = time * 1000;
    }

    /** Start up and shut down events */
    public void startStopEvents(EventScheduler s, GlobalController g) {
        // simulation start
        StartSimulationEvent e0 = new StartSimulationEvent(0, this);

        s.addEvent(e0);

        // simulation end
        EndSimulationEvent e = new EndSimulationEvent(timeToEnd_, this);
        s.addEvent(e);
    }

    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g) {
        StartRouterEvent e2 = new StartRouterEvent(250, this);

        s.addEvent(e2);
    }

    /** Add or remove events following a simulation event */
    public void preceedEvent(Event e, EventScheduler s, GlobalController g) {
    }

    /** Add or remove events following a simulation event */
    public void followEvent(Event e, EventScheduler s, JSONObject response, GlobalController g) {
    }

}