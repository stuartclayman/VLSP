package ikms.core;

/**
 * A Response holds a boolean which determines
 * if the response to a call was successful, together with
 * an optional response string.
 */
public class Response {
    // The boolean
    boolean result;

    // The String
    String msg;

    /**
     * Construct an Response
     */
    public Response(boolean b, String s) {
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
    public String toString() {
        return "Response: " + result + " -> " + msg;
    }
}
