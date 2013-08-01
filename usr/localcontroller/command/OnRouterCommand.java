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
 * The ON_ROUTER command.
 */
public class OnRouterCommand extends LocalCommand {
    /**
     * Construct a OnRouterCommand.
     */
    public OnRouterCommand() {
        super(MCRP.ON_ROUTER.CMD);
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

            if (args.length < 4) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected three or more arguments for ON_ROUTER Command: ON_ROUTER router_id className args");

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
                    jsobj.put("error", "Argument for ON_ROUTER command must be int");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

                String className = args[2];

                // collect args
                String[] cmdArgs = new String[args.length - 3];

                for (int a = 3; a < args.length; a++) {
                    cmdArgs[a-3] = args[a];
                }


                // get controller to do work
                JSONObject result = controller.onRouter(routerID, className, cmdArgs);

                if (result != null) {
                    JSONObject jsobj = result;

                    out.println(jsobj.toString());
                    response.close();

                    return true;
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "ON_ROUTER. ERROR with " + value);

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