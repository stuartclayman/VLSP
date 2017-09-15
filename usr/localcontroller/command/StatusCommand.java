package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * A StatusCommand
 * This returns a status
 */
public class StatusCommand extends LocalCommand {
    /**
     * Construct a StatusCommand
     */
    public StatusCommand() {
        super(MCRP.STATUS.CMD);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();


            JSONObject jsobj = controller.getStatus();

            out.println(jsobj.toString());
            response.close();

            return true;
        } catch (Exception ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        }

        return false;
    }

}
