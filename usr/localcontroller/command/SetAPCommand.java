package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.*;
import java.util.Scanner;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

/**
 * The SET_AP command selets whether a router is or is not
 * an aggregation point
 */
public class SetAPCommand extends LocalCommand {
    /**
     * Construct a SetAddressCommand.
     */
    public SetAPCommand() {
        super(MCRP.SET_AP.CMD);
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

            String[] parts = value.split(" ");

            if (parts.length != 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "SET_AP command requires GID and AP GID");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            // process args
            int GID;
            int AP;
            try {
                GID = Integer.parseInt(parts[1]);
                AP = Integer.parseInt(parts[2]);
            } catch (Exception e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "SET_AP command requires GID and AP GID");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            // set AP
            if (controller.setAP(GID, AP)) {
                JSONObject jsobj = new JSONObject();

                jsobj.put("msg", GID+" has set AP to "+AP);
                jsobj.put("success", Boolean.TRUE);
                out.println(jsobj.toString());
                response.close();

                return true;
            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Incorrect GID number "+GID);

                out.println(jsobj.toString());
                response.close();

                return false;
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