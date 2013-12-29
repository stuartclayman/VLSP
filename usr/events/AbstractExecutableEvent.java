package usr.events;

import usr.engine.EventEngine;
import us.monoid.json.JSONObject;

/** Class represents a global controller event*/
public abstract class AbstractExecutableEvent extends AbstractEvent implements ExecutableEvent {
    /**
     * Important constructor to create Event.
     */
    protected AbstractExecutableEvent(long t, EventEngine eng) {
        super(t, eng);
    }
        
    /** Execute the event and return a JSON object with information*/
    @Override
    public abstract JSONObject execute(EventDelegate obj) throws InstantiationException;

}
