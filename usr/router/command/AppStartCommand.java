package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.ApplicationHandle;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;


/**
 * The APP_START command starts an application in the same
 * JVM as a Router.
 */
public class AppStartCommand extends RouterCommand {
    /**
     * Construct a AppStartCommand
     */
    public AppStartCommand() {
        super(MCRP.APP_START.CMD, MCRP.APP_START.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        PrintStream out;

        try {
            out = response.getPrintStream();
        } catch (IOException ioe) {
            return false;
        }

        try {

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String rest = value.substring(MCRP.APP_START.CMD.length()).trim();

            if (rest.equals("")) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "APP_START needs application class name");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {

                ApplicationResponse result = controller.appStart(rest);

                if (result.isSuccess()) {

                    ApplicationHandle appH = controller.findAppInfo(result.getMessage());

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("aid", appH.getID());
                    jsobj.put("name", appH.getName());
                    jsobj.put("startTime", appH.getStartTime());

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

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "AppStartCommand Exception: " + e.getClass().getName());
            e.printStackTrace();

            response.setCode(302);
            try {
            	JSONObject jsobj = new JSONObject();
            	jsobj.put("error", "Exception " + e.getClass().getName());

            	out.println(jsobj.toString());
            	response.close();
            } catch (IOException ie) {

            } catch (JSONException je) {

            }
            return false;
        }

    }

}
