package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.LocalHostInfo;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The CHECK_LOCAL_CONTROLLER command.
 */
public class LocalCheckCommand extends LocalCommand {
    /**
     * Construct a LocalCheckCommand.
     */
    public LocalCheckCommand() {
        super(MCRP.CHECK_LOCAL_CONTROLLER.CMD);
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

            String rest = value;
            String [] args = value.split(" ");

            if (args.length != 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Local Check Command has wrong arguments " + rest);

                jsobj.put("success", Boolean.FALSE);
                out.println(jsobj.toString());
                response.close();

                return false;
            }


            String hostName = args[1];
            int port = Integer.parseInt(args[2]);
            LocalHostInfo gc = null;
            try {
                gc = new LocalHostInfo(hostName, port);
            } catch (UnknownHostException e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Cannot find host info for LOCAL_CHECK_COMMAND "+e.getMessage());

                jsobj.put("success", Boolean.FALSE);
                out.println(jsobj.toString());
                response.close();

                return false;
            }

            controller.aliveMessage(gc);

            JSONObject jsobj = new JSONObject();

            jsobj.put("msg", "Ping from global controller received.");
            jsobj.put("success", Boolean.TRUE);
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
