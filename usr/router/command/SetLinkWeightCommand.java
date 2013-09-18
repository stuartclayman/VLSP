package usr.router.command;


import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;
import usr.router.NetIF;


/**
 * The SET_LINK_WEIGHT command.
 */
public class SetLinkWeightCommand extends RouterCommand {
    /**
     * Construct an SetLinkWeightCommand
     */
    public SetLinkWeightCommand() {
        super(MCRP.SET_LINK_WEIGHT.CMD, MCRP.SET_LINK_WEIGHT.CODE, MCRP.ERROR.ERROR);
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


            // check command
            String[] parts = value.split(" ");

            //Logger.getLogger("log").logln(USR.ERROR, "END LINK ENTRY");
            if (parts.length != 3) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "INVALID SET_LINK_WEIGHT command: " + request);
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "SET_LINK_WEIGHT wrong no of args");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            String rId = parts[1];
            String weightStr = parts[2];
            int weight = Integer.MIN_VALUE;

            try {
                weight = Integer.parseInt(weightStr);

            } catch (NumberFormatException nfe) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "SET_LINK_WEIGHT BAD weight for link is not a number " + weightStr);

                out.println(jsobj.toString());
                response.close();

                return false;
            }


            // try and find the NetIF by ID
            NetIF netif = controller.findNetIF(rId);

            if (netif == null) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "SET_LINK_WEIGHT cannot find link to "+rId);

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            // now set weight
            if (weight != Integer.MIN_VALUE) {
                controller.setNetIFWeight(rId, weight);

                JSONObject jsobj = new JSONObject();

                jsobj.put("id", rId);
                jsobj.put("weight", weight);
                out.println(jsobj.toString());
                response.close();

                return true;
            } else {

                JSONObject jsobj = new JSONObject();
                jsobj.put("msg", "not set");

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
