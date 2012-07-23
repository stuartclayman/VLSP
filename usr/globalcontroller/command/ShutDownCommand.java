package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

/**
 * A ShutDownCommand
 */
public class ShutDownCommand extends GlobalCommand {
    /**
     * Construct a ShutDownCommand
     */
    public ShutDownCommand() {
        super(MCRP.SHUT_DOWN.CMD);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            controller.shutDownCommand();

            JSONObject jsobj = new JSONObject();
            jsobj.put("msg", "Shut Down Sent to Local Controller -- will be processed next in queue");

            out.println(jsobj.toString());
            response.close();

            return true;

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        } finally {
            return false;
        }
    }

}
