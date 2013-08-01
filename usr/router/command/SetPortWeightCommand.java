package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.*;
import java.util.Scanner;


/**
 * The SET_PORT_WEIGHT command.
 * SET_PORT_WEIGHT port weight
 * SET_PORT_WEIGHT port0 15
 */
public class SetPortWeightCommand extends RouterCommand {
    /**
     * Construct a SetPortWeightCommand.
     */
    public SetPortWeightCommand() {
        super(MCRP.SET_PORT_WEIGHT.CMD, MCRP.SET_PORT_WEIGHT.CODE, MCRP.SET_PORT_WEIGHT.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String rest = value.substring(MCRP.SET_PORT_WEIGHT.CMD.length()).trim();
            String[] parts = rest.split(" ");

            if (parts.length == 2) {

                String routerPortName = parts[0];
                String weightStr = parts[1];

                // find port
                String portNo;

                if (routerPortName.startsWith("port")) {
                    portNo = routerPortName.substring(4);
                } else {
                    portNo = routerPortName;
                }

                Scanner scanner = new Scanner(portNo);
                int p = scanner.nextInt();
                RouterPort routerPort = controller.getPort(p);

                if (routerPort == null || routerPort == RouterPort.EMPTY) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", getName() + " invalid port " + routerPortName);

                    out.println(jsobj.toString());
                    response.close();

                    return false;

                } else {

                    // instantiate the weight
                    int weight = Integer.MIN_VALUE;
                    try {
                        weight = Integer.parseInt(weightStr);
                    } catch (NumberFormatException nfe) {
                        response.setCode(302);

                        JSONObject jsobj = new JSONObject();
                        jsobj.put("error", getName() + " weight is not a number " + weightStr);

                        out.println(jsobj.toString());
                        response.close();

                        return false;

                    }

                    // set weight on netIF in port
                    if (weight != Integer.MIN_VALUE) {
                        NetIF netIF = routerPort.getNetIF();
                        netIF.setWeight(weight);

                        JSONObject jsobj = new JSONObject();

                        jsobj.put("name", routerPortName);
                        jsobj.put("weight", weight);
                        out.println(jsobj.toString());
                        response.close();

                        return true;
                    }
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

        finally {
            return false;
        }

    }

}