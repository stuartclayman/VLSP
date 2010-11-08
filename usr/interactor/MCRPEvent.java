// MCRPEvent.java

package usr.interactor;
import usr.logging.*;

import java.util.EventObject;

/**
 * An MCRPEvent is generated when the router sends
 * an event via the protocol.
 */
public class MCRPEvent extends EventObject {
    // The type of the event
    private MCRPEventType type;

    // The object sent back.
    private Object obj;

    public enum MCRPEventType { LINK_DIED, LINK_RESTARTED, ROUTER_STATS };

    /**
     * Construct an MCRPEvent given a source object.
     */
    public MCRPEvent(Object source, MCRPEventType type,  Object obj) {
	super(source);
	this.type = type;
	this.obj = obj;
    }

    /**
     * Get the event type.
     */
    public MCRPEventType getType() {
	return type;
    }

    /**
     * Get the object
     */
    public Object getObject() {
	return obj;
    }

    /**
     * MCRPEvent to String.
     */
    public String toString() {
	return "MCRPEvent " + type +  " with: " + obj;
    }
}
