package usr.globalcontroller.command;

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
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String rest = value.substring(MCRP.SEND_ROUTER_STATS.CMD.length()).trim();


            // Stats are posted in
            // So collect them
            String stats  = request.getContent();
            controller.receiveRouterStats(stats);

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

        return false;



    }

}
