// MCRPResponse.java

package usr.interactor;

import usr.logging.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An MCRP response sent from the ManagementConsole of a Router.
 */
public class MCRPResponse {
    // The list of all the replies for this response
    List<String[]>  list = new ArrayList<String[]>();

    // The response code
    String code = null;

    /**
     * Construct a MCRPResponse
     */
    public MCRPResponse() {
    }

    /**
     * Construct a MCRPResponse with a reply
     */
    public MCRPResponse(String[] reply) throws MCRPException {
        add(reply);
    }

    /**
     * Add a reply to this response
     */
    public void add(String[] reply) throws MCRPException {
        // patch up the code
        String replyCode = reply[0];

        if (code == null) {
            code = replyCode;
        } else if (code.equals(replyCode)) {
            // they are the same which is good
        } else {
            // they are not the same
            // which means the protocol failed
            throw new MCRPException("Bad reply code. Expected " + code + ". Got " + replyCode);
        }

        list.add(reply);

    }

    /**
     * Get the response code for this response
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the number of replies in this response.
     */
    public int getReplies() {
        return list.size();
    }

    /**
     * Get the nth reply in the response.
     */
    public String[] get(int n) {
        return list.get(n);
    }

    /**
     * Determine if a response is an event
     */
    public boolean isEvent() {
        if (code == null) {
            return false;
        } else if (code == "") {
            return false;
        } else if (code.charAt(0) == '7') {
            // the fist char of the code is a 7
            // so it's an event
            return true;
        } else {
            return false;
        }
    }

}
