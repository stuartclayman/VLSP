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
 * The SetLinkWeight command.
 */
public class SetLinkWeightCommand extends LocalCommand {
    /**
     * Construct a SetLinkWeightCommand.
     */
    public SetLinkWeightCommand() {
        super(MCRP.SET_LINK_WEIGHT.CMD);
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

            if (args.length != 4) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected two arguments for End Link Command");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            LocalHostInfo r1;
            String r2Addr;
            int weight;

            try {
                r1 = new LocalHostInfo(args[1]);
                r2Addr = args[2];
            } catch (UnknownHostException e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT PARSE HOST INFO FOR SET_LINK_WEIGHT "+e.getMessage());

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            try {
                weight = Integer.parseInt(args[3]);

            } catch (NumberFormatException nfe) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "BAD weight for link: "+nfe.getMessage());

                out.println(jsobj.toString());
                response.close();

                return false;
            }


            if (controller.setLinkWeight(r1, r2Addr, weight)) {
                JSONObject jsobj = new JSONObject();

                jsobj.put("msg", "SET LINK WEIGHT from "+r1+" to Id "+r2Addr + " weight: " + weight);
                jsobj.put("success", Boolean.TRUE);
                out.println(jsobj.toString());
                response.close();

                return true;
            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT SET LINK WEIGHT "+ r1 + " -> " + r2Addr + " to " + weight);

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
