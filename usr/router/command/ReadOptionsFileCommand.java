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
 * The READ_OPTIONS_FILE command
 */
public class ReadOptionsFileCommand extends RouterCommand {
    /**
     * Construct a GetAddressCommand.
     */
    public ReadOptionsFileCommand() {
        super(MCRP.READ_OPTIONS_FILE.CMD, MCRP.READ_OPTIONS_FILE.CODE,
              MCRP.READ_OPTIONS_FILE.ERROR);
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
            String[] args = value.split(" ");

            if (args.length != 2) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "READ_OPTIONS_FILE requires two arguments");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {
                if (controller.readOptionsFile(args[1].trim())) {
                    JSONObject jsobj = new JSONObject();

                    jsobj.put("response", "Read Options File");
                    out.println(jsobj.toString());
                    response.close();

                    return true;

                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "Cannot read options file");

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