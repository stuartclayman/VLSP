/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.events.EventScheduler;
import usr.globalcontroller.GlobalController;

public class NullEventEngine implements EventEngine {
    /** Contructor */
    public NullEventEngine() {
    }

    /** Initial events to add to schedule */
    public void startStopEvents(EventScheduler s, GlobalController g) {
    }

    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g) {
    }

    /** Add or remove events following a simulation event */
    public void preceedEvent(Event e, EventScheduler s, GlobalController g) {
    }

    /** Add or remove events following a simulation event */
    public void followEvent(Event e, EventScheduler s, JSONObject js, GlobalController g) {
    }

}