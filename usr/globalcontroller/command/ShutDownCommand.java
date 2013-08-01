package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import usr.events.*;
import java.util.concurrent.*;

/**
 * A ShutDownCommand
 */
public class ShutDownCommand extends GlobalCommand {
    /**
     * Construct a ShutDownCommand
     */
    public ShutDownCommand() {
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