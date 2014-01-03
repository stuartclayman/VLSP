package usr.globalcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONObject;
import usr.events.EndSimulationEvent;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * A ShutDownCommand
 * This creates an EndSimulationEvent.
 */
public class ShutDownEventCommand extends GlobalCommand {
    /**
     * Construct a ShutDownEventCommand
     */
    public ShutDownEventCommand() {
        super(MCRP.SHUT_DOWN.CMD);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            EndSimulationEvent shutdown = new EndSimulationEvent(System.currentTimeMillis());

            JSONObject jsobj = controller.executeEvent(shutdown);

            out.println(jsobj.toString());
            response.close();

            return true;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + ioe.getMessage());
        }

        return false;
    }

}
