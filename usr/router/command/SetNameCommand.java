package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;


/**
 * The SET_NAME command.
 * SET_NAME name
 * SET_NAME Router-47
 */
public class SetNameCommand extends RouterCommand {
    /**
     * Construct a SetNameCommand.
     */
    public SetNameCommand() {
        super(MCRP.SET_NAME.CMD, MCRP.SET_NAME.CODE, MCRP.ERROR.CODE);
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
            String name = value.substring(MCRP.SET_NAME.CMD.length()).trim();

            // set name
            boolean nameSet = controller.setName(name);


            JSONObject jsobj = new JSONObject();
            jsobj.put("name", name);

            out.println(jsobj.toString());
            response.close();

            return true;

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