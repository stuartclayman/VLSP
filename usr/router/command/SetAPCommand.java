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

/**
 * The SET_AP command selets whether a router is or is not
 * an aggregation point
 */
public class SetAPCommand extends RouterCommand {
    /**
     * Construct a SetAPCommand.
     */
    public SetAPCommand() {
        super(MCRP.SET_AP.CMD, MCRP.SET_AP.CODE, MCRP.SET_AP.ERROR);
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
            // strip off COMMAND
            String[] parts = value.split(" ");

            if (parts.length < 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "SET_AP command requires GID and AP GID");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {
                int GID;
                int AP;
                try {
                    GID = Integer.parseInt(parts[1]);
                    AP = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "SET_AP command requires GID and AP GID as numbers");

                    out.println(jsobj.toString());
                    response.close();

                    return false;

                }

                // now try and get some context data into ctxArgs
                String[] ctxArgs = new String[parts.length - 3];

                for (int a = 3; a < parts.length; a++) {
                    ctxArgs[a-3] = parts[a];
                }



                // set AP
                controller.setAP(GID, AP, ctxArgs);

                JSONObject jsobj = new JSONObject();

                jsobj.put("gid", GID);
                jsobj.put("ap", AP);
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
