package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Scanner;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The MONITORING_START command starts monitoring on specified address
 * and port, with Measurements every N seconds.
 *
 * MONITORING_START 192.168.7.23:4545 10
 */
public class MonitoringStartCommand extends LocalCommand {
    /**
     * Construct a MonitoringStartCommand
     */
    public MonitoringStartCommand() {
        super(MCRP.MONITORING_START.CMD);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {

        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String rest = value.substring(MCRP.MONITORING_START.CMD.length()).trim();

            String [] parts = rest.split(" ");

            // get address and port
            // check ip addr spec

            // process host and port
            String host = parts[0];

            Scanner sc = new Scanner(parts[1]);
            int portNumber;

            try {
                portNumber = sc.nextInt();
                sc.close();
            } catch (Exception e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "MONITORING_START invalid port: " + parts[1]);

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            // get timeout for Probe
            int timeout;

            // get timeout
            sc = new Scanner(parts[2]);

            try {
                timeout = sc.nextInt();
            } catch (Exception e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "MONITORING_START invalid timeout: " + parts[1]);

                out.println(jsobj.toString());
                response.close();
                sc.close();
                return false;

            }

            // if we get here all the args seem OK
            InetSocketAddress socketAddress = new InetSocketAddress(host, portNumber);


            controller.startMonitoring(socketAddress, timeout);

            JSONObject jsobj = new JSONObject();

            jsobj.put("response", "Monitoring Started");
            jsobj.put("success", Boolean.TRUE);
            out.println(jsobj.toString());
            response.close();
            sc.close();
            return true;


        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;


    }

}
