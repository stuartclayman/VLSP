package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;


/**
 * The Router OK command simply checks a router is functioning
 */
public class RouterOKCommand extends RouterCommand {
    /**
     * Constructor
     */
    public RouterOKCommand() {
        super(MCRP.ROUTER_OK.CMD, MCRP.ROUTER_OK.CODE, MCRP.ERROR.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = new JSONObject();
            jsobj.put("ok", "ok");

            out.println(jsobj.toString());
            response.close();

            return true;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        finally {
            return false;
        }
    }

}