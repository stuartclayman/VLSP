package usr.engine;

/** Class representing an excpetion raised in constructing event engine*/
public class EventEngineException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6789039253589223524L;

	public EventEngineException() {
        super();
    }

    public EventEngineException(String message) {
        super(message);
    }

}