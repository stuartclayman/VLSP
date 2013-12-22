package usr.engine;

import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.APcontroller.APController;

/** Interface for Engines which send nodes to an APController.
 */
public interface APWarmUp {
    /**
     * Add nodes simply to prime AP controllers lifetime estimation
     */
    public void warmUp(EventScheduler s, long period, APController apController, EventDelegate gc);
}
