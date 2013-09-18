package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The NEW_ROUTER command.
 */
public class NewRouterCommand extends LocalCommand {
    /**
     * Construct a NewRouterCommand.
     */
    public NewRouterCommand() {
        super(MCRP.NEW_ROUTER.CMD);
    }

    /**
     * Evaluate the Command.  NEW_ROUTER id mcrpPort r2rPort [address] [name]
     */
    @Override
	public boolean evaluate(Request request, Response response) {

        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);

            String [] args = value.split(" ");

            if (args.length != 4 && args.length != 5 && args.length != 6) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error",
                          "Expected four, five or six arguments for New Router Command: NEW_ROUTER id mcrpPort r2rPort [address] [name].");

                out.println(jsobj.toString());
                response.close();
                return false;
            }
            int rId, port1, port2;
            String address = null;
            String name = null;

            try {
                rId = Integer.parseInt(args[1]);
                port1 = Integer.parseInt(args[2]);
                port2 = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Argument for new router command must be int");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            if (args.length == 5 || args.length == 6) {
                // there is a address too
                address = args[4];
            }

            if (args.length == 6) {
                // there is a name too
                name = args[5];
            }

            String routerName = controller.requestNewRouter(rId, port1, port2, address, name);

            if (routerName != null) {
                // find BasicRouterInfo from controller map
                // and return all relevant values
                BasicRouterInfo bri = controller.findRouterInfo(rId);

                JSONObject jsobj = new JSONObject();

                jsobj.put("routerID", bri.getId());
                jsobj.put("name", bri.getName());
                jsobj.put("address", bri.getAddress());
                jsobj.put("mgmtPort", bri.getManagementPort());
                jsobj.put("r2rPort", bri.getRoutingPort());

                out.println(jsobj.toString());
                response.close();

                return true;

            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT START NEW ROUTER");

                out.println(jsobj.toString());
                response.close();

                return false;
            }
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;

    }

}
