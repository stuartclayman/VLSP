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

    // The ID of the router client this event is associated with.
    private ID routerID;

    public enum MCRPEventType { LINK_DIED, LINK_RESTARTED };

    /**
     * Construct an MCRPEvent given a source object.
     */
    public MCRPEvent(Object source, MCRPEventType type,  ID routerID) {
	super(source);
	this.type = type;
	this.routerID = routerID;
    }

    /**
     * Get the event type.
     */
    public MCRPEventType getType() {
	return type;
    }

    /**
     * Get the ID of the router client this event is associated with.
     */
    public ID getRouterID() {
	return routerID;
    }

    /**
     * MCRPEvent to String.
     */
    public String toString() {
	return "MCRPEvent " + type +  " from router: " + routerID;
    }
}
