package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.common.LocalHostInfo;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * The SendRouterStatsCommand command -- receives stats from local controller
 */
public class SendRouterStatsCommand extends GlobalCommand  {
    /**
     * Construct a SendRouterStatsCommand
     */
    public SendRouterStatsCommand() {
        super(MCRP.SEND_ROUTER_STATS.CMD);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String rest = value.substring(MCRP.SEND_ROUTER_STATS.CMD.length()).trim();

            controller.receiveRouterStats(rest);

            JSONObject jsobj = new JSONObject();

            jsobj.put("msg", "STATS RECEIVED");
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