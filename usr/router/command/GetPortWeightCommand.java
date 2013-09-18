package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;
import usr.router.NetIF;
import usr.router.RouterPort;


/**
 * The GET_PORT_WEIGHT command.
 * GET_PORT_WEIGHT port
 * GET_PORT_WEIGHT port0
 */
public class GetPortWeightCommand extends RouterCommand {
    /**
     * Construct a GetPortWeightCommand.
     */
    public GetPortWeightCommand() {
        super(MCRP.GET_PORT_WEIGHT.CMD, MCRP.GET_PORT_WEIGHT.CODE, MCRP.GET_PORT_WEIGHT.ERROR);
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
            String rest = value.substring(MCRP.GET_PORT_WEIGHT.CMD.length()).trim();
            String[] parts = rest.split(" ");

            if (parts.length == 1) {

                String routerPortName = parts[0];

                // find port
                String portNo;

                if (routerPortName.startsWith("port")) {
                    portNo = routerPortName.substring(4);
                } else {
                    portNo = routerPortName;
                }

                Scanner scanner = new Scanner(portNo);
                int p = scanner.nextInt();
                scanner.close();
                RouterPort routerPort = controller.getPort(p);

                if (routerPort == null || routerPort == RouterPort.EMPTY) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", getName() + " invalid port " + routerPortName);

                    out.println(jsobj.toString());
                    response.close();

                    return false;

                } else {

                    // get weight on netIF in port
                    NetIF netIF = routerPort.getNetIF();
                    int weight = netIF.getWeight();
                    JSONObject jsobj = new JSONObject();

                    jsobj.put("weight", weight);
                    out.println(jsobj.toString());
                    response.close();

                    return true;
                }

            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", getName() + " wrong no of args ");

                out.println(jsobj.toString());
                response.close();

                return false;

            }

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;

    }

}