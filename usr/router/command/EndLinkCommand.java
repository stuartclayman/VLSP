package usr.router.command;

import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;
import usr.router.EndLink;


/**
 * The END_LINK command.
 */
public class EndLinkCommand extends RouterCommand {
    /**
     * Construct an EndLinkCommand
     */
    public EndLinkCommand() {
        super(MCRP.END_LINK.CMD, MCRP.END_LINK.CODE, MCRP.END_LINK.ERROR);
    }

    /**
     * Evaluate the Command.
     */
	@Override
	public boolean evaluate(Request request, Response response) {
        try {
            // Call EndLink
            EndLink ender = new EndLink(controller, request, response);

            ender.run();
            //managementConsole.addRequest(new Request(sc, req));

            return true;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;
    }

}