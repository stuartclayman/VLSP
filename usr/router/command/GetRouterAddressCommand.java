package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import usr.net.Address;

/**
 * The GET_ROUTER_ADDRESS command.
 */
public class GetRouterAddressCommand extends RouterCommand {
    /**
     * Construct a GetRouterAddressCommand
     */
    public GetRouterAddressCommand() {
        super(MCRP.GET_ROUTER_ADDRESS.CMD, MCRP.GET_ROUTER_ADDRESS.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            Address a = controller.getAddress();


            JSONObject jsobj = new JSONObject();
            jsobj.put("address", a.asTransmitForm());

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