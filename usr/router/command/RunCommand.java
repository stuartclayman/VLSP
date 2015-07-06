package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;


/**
 * The RUN command -- allows applications to run on the router
 */
public class RunCommand extends RouterCommand {
    /**
     * Construct a RunCommand.
     */
    public RunCommand() {
        super(MCRP.RUN.CMD, MCRP.RUN.CODE, MCRP.ERROR.CODE);
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
            String rest = value.substring(MCRP.RUN.CMD.length()).trim();

            if (rest.equals("")) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "RUN Must supply command name and args");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {

                ApplicationResponse result = controller.appStop(rest);

                if (result.isSuccess()) {

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("response", result.getMessage());
                    out.println(jsobj.toString());
                    response.close();

                    return true;

                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", result.getMessage() + " for " + rest);

                    out.println(jsobj.toString());
                    response.close();

                    return false;

                }

            }
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;


    }

}
