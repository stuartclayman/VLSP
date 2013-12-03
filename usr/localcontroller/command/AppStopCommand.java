package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import java.util.Scanner;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

/**
 * The APP_STOP command.
 */
public class AppStopCommand extends LocalCommand {
    /**
     * Construct a AppStopCommand.
     */
    public AppStopCommand() {
        super(MCRP.APP_STOP.CMD);
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

            String [] args = value.split(" ");

            if (args.length < 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected three or more arguments for APP_STOP Command: APP_STOP router_id appName ");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {
                Scanner sc = new Scanner(args[1]);
                int routerID;

                if (sc.hasNextInt()) {
                    routerID = sc.nextInt();
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "Argument for APP_STOP command must be int");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

                String appName = args[2];


                // get controller to do work
                JSONObject result = controller.appStop(routerID, appName);

                if (result != null) {
                    JSONObject jsobj = result;

                    out.println(jsobj.toString());
                    response.close();

                    return true;
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "APP_STOP. ERROR with " + value);

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }
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
