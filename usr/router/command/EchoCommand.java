package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.protocol.MCRP;


/**
 * The ECHO command.
 */
public class EchoCommand extends RouterCommand {
    /**
     * Construct a GetNameCommand.
     */
    public EchoCommand() {
        super(MCRP.ECHO.CMD, MCRP.ECHO.CODE,
              MCRP.ECHO.CODE);
    }

    /**
     * Evaluate the Command.
     */
	@Override
	public boolean evaluate(Request request, Response response) {
        Address addr;

        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String [] args = value.split(" ");

            if (args.length != 2) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "REQUIRE ADDRESS");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {

                try {
                    addr = AddressFactory.newAddress(args[1].trim());
                } catch (Exception e) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "CANNOT PARSE ADDRESS");

                    out.println(jsobj.toString());
                    response.close();

                    return false;

                }

                if (controller.echo(addr)) {
                    JSONObject jsobj = new JSONObject();

                    jsobj.put("echo", addr.toString());
                    out.println(jsobj.toString());
                    response.close();

                    return true;
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "No route to router "+addr);

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