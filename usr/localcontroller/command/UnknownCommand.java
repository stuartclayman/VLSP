package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;

/**
 * The command to execute if the incoming command is unknown.
 */
public class UnknownCommand extends LocalCommand {
    /**
     * Construct a UnknownCommand
     */
    public UnknownCommand() {
        super("__UNKNOWN__");
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            response.setCode(302);

            JSONObject jsobj = new JSONObject();
            jsobj.put("error", "UnknownCommand");

            out.println(jsobj.toString());
            response.close();

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;
    }

}