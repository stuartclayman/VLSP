package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicRouterInfo;
import usr.common.LocalHostInfo;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The END_ROUTER_COMMAND command.
 */
public class EndRouterCommand extends LocalCommand {
    /**
     * Construct a EndRouterCommand.
     */
    public EndRouterCommand() {
        super(MCRP.ROUTER_SHUT_DOWN.CMD);
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

            String [] args = value.split(" ");

            if (args.length != 2) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected argument for End Router Command");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            LocalHostInfo lhi = null;
            try {
                lhi = new LocalHostInfo(args[1]);
            } catch (UnknownHostException e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT PARSE HOST INFO FOR END_ROUTER "+e.getMessage());

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            // end router
            // find BasicRouterInfo from controller
            // based on LocalHostInfo port
            BasicRouterInfo bri = controller.findRouterInfoByPort(lhi.getPort());

            if (controller.endRouter(lhi)) {
                JSONObject jsobj = new JSONObject();

                jsobj.put("routerID", bri.getId());
                jsobj.put("name", bri.getName());
                jsobj.put("address", bri.getAddress());
                jsobj.put("mgmtPort", bri.getManagementPort());
                jsobj.put("r2rPort", bri.getRoutingPort());
                jsobj.put("msg", "ROUTER ENDED "+lhi);
                jsobj.put("success", Boolean.TRUE);
                out.println(jsobj.toString());
                response.close();

                return true;
            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT END ROUTER "+lhi);

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