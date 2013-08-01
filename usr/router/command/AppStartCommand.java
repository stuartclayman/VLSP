package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import usr.common.Pair;
import us.monoid.json.*;
import usr.applications.ApplicationResponse;
import usr.applications.ApplicationHandle;

/**
 * The APP_START command starts an application in the same
 * JVM as a Router.
 */
public class AppStartCommand extends RouterCommand {
    /**
     * Construct a AppStartCommand
     */
    public AppStartCommand() {
        super(MCRP.APP_START.CMD, MCRP.APP_START.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(
                    request.getPath().getPath(), "UTF-8");

            // strip off /command
            String value = path.substring(9);

            // strip off COMMAND
            String rest
                = value.substring(MCRP.APP_START.CMD.length()).trim();

            if (rest.equals("")) {
                response.setCode(404);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error",
                          "APP_START needs application class name");

                out.println(jsobj.toString());
                response.close();

                return false;
            } else {
                ApplicationResponse result = controller.appStart(rest);

                if (result.isSuccess()) {
                    ApplicationHandle appH = controller.findAppInfo(
                            result.getMessage());

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("aid", appH.getID());
                    jsobj.put("name", appH.getName());
                    jsobj.put("startTime", appH.getStartTime());

                    out.println(jsobj.toString());
                    response.close();

                    return true;
                } else {
                    response.setCode(404);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error",
                              result.getMessage() + " for " + rest);

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }
            }
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + jex.getMessage());
        }

        finally {
            return false;
        }
    }

}