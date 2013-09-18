package usr.router.command;

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
 * The GET_CONNECTION_PORT command.
 */
public class GetConnectionPortCommand extends RouterCommand {
    /**
     * Construct a GetConnectionPortCommand
     */
    public GetConnectionPortCommand() {
        super(MCRP.GET_CONNECTION_PORT.CMD, MCRP.GET_CONNECTION_PORT.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            int port = controller.getConnectionPort();

            JSONObject jsobj = new JSONObject();
            jsobj.put("port", Integer.toString(port));

            out.println(jsobj.toString());
            response.close();

            return true;

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;
    }

}