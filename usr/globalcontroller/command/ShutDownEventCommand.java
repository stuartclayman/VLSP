package usr.globalcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONObject;
import usr.events.EndSimulationEvent;
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
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            EndSimulationEvent shutdown
                = new EndSimulationEvent(
                        System.currentTimeMillis(), null);

            JSONObject jsobj;
            try {
                jsobj = controller.executeEvent(shutdown);
            } catch (InterruptedException ie) {
                jsobj = controller.commandError(
                        "Semaphor acquisition interrupted "
                        + ie.getMessage());
            } catch (TimeoutException te) {
                jsobj = controller.commandError(
                        "Semaphor timeout " + te.getMessage());
            } catch (InstantiationException ise) {
                jsobj = controller.commandError(
                        "Could not shutdown " + ise.getMessage());
            }

            out.println(jsobj.toString());
            response.close();

            return true;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + ioe.getMessage());
        }

        finally {
            return false;
        }
    }

}
