package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The RequestRouterStatsCommand command.
 */
public class RequestRouterStatsCommand extends LocalCommand {
    // the original request
    String request;

    /**
     * Construct a RequestRouterStatsCommand.
     */
    public RequestRouterStatsCommand() {
        super(MCRP.REQUEST_ROUTER_STATS.CMD);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            if (controller.getGlobalControllerInteractor() == null) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "RequestRouterStatsCommand: No global controller present");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {

                List<String> list = controller.getRouterStats();
                controller.sendRouterStats(list);

                JSONObject jsobj = new JSONObject();
                jsobj.put("msg", "REQUEST FOR STATS RECEIVED");

                out.println(jsobj.toString());
                response.close();

                return true;

            }
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;
    }

}