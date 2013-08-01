package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import java.util.Scanner;
import java.net.InetSocketAddress;

/**
 * The MONITORING_STOP command stops monitoring
 * MONITORING_STOP
 */
public class MonitoringStopCommand extends LocalCommand {
    /**
     * Construct a MonitoringStopCommand
     */
    public MonitoringStopCommand() {
        super(MCRP.MONITORING_STOP.CMD);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {

        try {
            PrintStream out = response.getPrintStream();

            controller.stopMonitoring();

            JSONObject jsobj = new JSONObject();

            jsobj.put("response", "Monitoring Stopped");
            jsobj.put("success", Boolean.TRUE);
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