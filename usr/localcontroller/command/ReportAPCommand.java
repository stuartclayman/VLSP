package usr.localcontroller.command;

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
 * The REPORT_AP command selets whether a router is or is not
 * an aggregation point
 */
public class ReportAPCommand extends LocalCommand {
    /**
     * Construct a SetAddressCommand.
     */
    public ReportAPCommand() {
        super(MCRP.REPORT_AP.CMD);
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


            String[] parts = value.split(" ");

            if (parts.length != 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "REPORT_AP command requires GID and AP GID");

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
                jsobj.put("error", "REPORT_AP command requires GID and AP GID");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            if (controller.reportAP(GID, AP)) {
                JSONObject jsobj = new JSONObject();

                jsobj.put("msg", GID+" reports AP "+AP);
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

        return false;

    }

}