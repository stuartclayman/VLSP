package usr.router.command;

import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;
import usr.router.CreateConnection;


/**
 * The CREATE_CONNECTION command.
 */
public class CreateConnectionCommand extends RouterCommand {
    /**
     * Construct a CreateConnectionCommand.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a
     * connection weight of connection_weight
     */
    public CreateConnectionCommand() {
        super(MCRP.CREATE_CONNECTION.CMD, MCRP.CREATE_CONNECTION.CODE, MCRP.CREATE_CONNECTION.ERROR);
    }

    /**
     * Evaluate the Command.
     */
	@Override
	public boolean evaluate(Request request, Response response) {
        try {
            // Call CreateConnection
            CreateConnection creator = new CreateConnection(controller, request, response);
            creator.run();
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