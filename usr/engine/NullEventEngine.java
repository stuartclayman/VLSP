/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;

public class NullEventEngine implements EventEngine {
    /** Contructor */
    public NullEventEngine() {
    }

    /** Initial events to add to schedule */
    @Override
    public void startStopEvents(EventScheduler s, EventDelegate g) {
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
    @Override
    public void followEvent(Event e, EventScheduler s, JSONObject js, EventDelegate g) {
    }

    @Override
    public void finalEvents(EventDelegate obj) {
    }

}
