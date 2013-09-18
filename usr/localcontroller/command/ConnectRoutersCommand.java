package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.LocalHostInfo;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The ConnectRouters command.
 */
public class ConnectRoutersCommand extends LocalCommand {
    /**
     * Construct a ConnectRoutersCommand.
     */
    public ConnectRoutersCommand() {
        super(MCRP.CONNECT_ROUTERS.CMD);
    }

    /**
     * Evaluate the Command CONNECT_ROUTERS Router1 Router2 Weight [Name]
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

            if (args.length != 4 && args.length !=5) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected four or five arguments for Connect Routers Command");

                out.println(jsobj.toString());
                response.close();

                return false;

            }

            LocalHostInfo r1 = null, r2 = null;
            int weight;

            try {
                r1 = new LocalHostInfo(args[1]);     // e.g localhost:11003
                r2 = new LocalHostInfo(args[2]);     // e.g remote:12341
                weight = Integer.parseInt(args[3]);

            } catch (NumberFormatException nfe) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "BAD weight for link: "+nfe.getMessage());

                out.println(jsobj.toString());
                response.close();

                return false;

            } catch (Exception e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT DECODE HOST INFO FOR CONNECT ROUTER COMMAND"+e.getMessage());

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            String name = null;

            if (args.length == 5) {
                // there is a name too
                name = args[4];
            }


            JSONObject retVal = controller.connectRouters(r1, r2, weight, name);

            if (retVal != null) {

                out.println(retVal.toString());
                response.close();

                return true;

            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT CONNECT ROUTERS");

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
