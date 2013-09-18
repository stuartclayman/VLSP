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
 * The MONITORING_STOP command stops monitoring
 * MONITORING_STOP
 */
public class MonitoringStopCommand extends RouterCommand {
    /**
     * Construct a MonitoringStopCommand
     */
    public MonitoringStopCommand() {
        super(MCRP.MONITORING_STOP.CMD, MCRP.MONITORING_STOP.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {

        try {
            PrintStream out = response.getPrintStream();

            controller.stopMonitoring();

            JSONObject jsobj = new JSONObject();

            jsobj.put("response", "Monitoring Stopped");
            out.println(jsobj.toString());
            response.close();

            return true;

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;
    }

}