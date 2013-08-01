package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

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
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String[] parts = value.split(" ");

            if (parts.length != 3) {
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

                controller.setAP(GID, AP);

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

        finally {
            return false;
        }


    }

}