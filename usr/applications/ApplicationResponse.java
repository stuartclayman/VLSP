package usr.applications;


/**
 * An application response holds a boolean which determines
 * if the response to a call was successful, together with
 * an optional response string.
 */
public class ApplicationResponse {
    // The boolean
    boolean result;

    // The String
    String msg;

    /**
     * Construct an ApplicationResponse
     */
    public ApplicationResponse(boolean b, String s) {
        result = b;
        msg = s;
    }

    /**
     * Is the response a success.
     */
    public boolean isSuccess() {
        return result;
    }

    /**
     * Get the message.
     */
    public String getMessage() {
        return msg;
    }

    /**
     * toString
     */
    @Override
	public String toString() {
        return "ApplicationResponse: " + result + " -> " + msg;
    }

}