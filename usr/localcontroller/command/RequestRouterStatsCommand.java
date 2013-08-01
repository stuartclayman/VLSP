package usr.localcontroller.command;

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

        finally {
            return false;
        }
    }

}