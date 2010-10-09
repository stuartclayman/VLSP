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

    // The ID of the speech-dispatcher client this event is associated with.
    private ID clientID;

    public enum MCRPEventType { BEGIN, END, CANCEL, PAUSE, RESUME, INDEX_MARK };

    /**
     * Construct an MCRPEvent given a source object.
     */
    public MCRPEvent(Object source, MCRPEventType type,  ID clientID) {
	super(source);
	this.type = type;
	this.clientID = clientID;
    }

    /**
     * Get the event type.
     */
    public MCRPEventType getType() {
	return type;
    }

    /**
     * Get the ID of the speech-dispatcher client this event is associated with.
     */
    public ID getClientID() {
	return clientID;
    }

    /**
     * MCRPEvent to String.
     */
    public String toString() {
	return "MCRPEvent " + type +  " from client " + clientID;
    }
}
