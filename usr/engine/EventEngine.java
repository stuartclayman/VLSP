/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;

public interface EventEngine {
    /** Initial events to add to schedule */
    public void startStopEvents(EventScheduler s, EventDelegate obj);

    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, EventDelegate obj);

    /** Add or remove events following a simulation event */
    public void preceedEvent(Event e, EventScheduler s, EventDelegate obj);

    /** Add or remove events following a simulation event */
    public void followEvent(Event e, EventScheduler s, JSONObject response, EventDelegate obj);

    /** Final events to add to schedule */
    public void finalEvents(EventDelegate obj);

}
