package usr.engine;

/** Class representing an excpetion raised in constructing event engine*/
public class EventEngineException extends Exception {
    public EventEngineException() {
        super();
    }

    public EventEngineException(String message) {
        super(message);
    }

}