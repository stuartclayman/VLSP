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
 * The EndLink command.
 */
public class EndLinkCommand extends LocalCommand {
    /**
     * Construct a EndLinkCommand.
     */
    public EndLinkCommand() {
        super(MCRP.END_LINK.CMD);
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

            if (args.length != 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected two arguments for End Link Command");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            LocalHostInfo r1;
            String r2Addr;

            try {
                r1 = new LocalHostInfo(args[1]);
                r2Addr = args[2];
            } catch (UnknownHostException e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT PARSE HOST INFO FOR END_LINK "+e.getMessage());

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            if (controller.endLink(r1, r2Addr)) {
                JSONObject jsobj = new JSONObject();

                jsobj.put("msg", "LINK ENDED FROM "+r1+" to Id "+r2Addr);
                jsobj.put("success", Boolean.TRUE);
                out.println(jsobj.toString());
                response.close();

                return true;
            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "CANNOT END LINK "+ r1 + " -> " + r2Addr);

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