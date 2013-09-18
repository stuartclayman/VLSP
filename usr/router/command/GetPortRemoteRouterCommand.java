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
 * The GET_PORT_REMOTE_ROUTER command.
 * GET_PORT_REMOTE_ROUTER port
 * GET_PORT_REMOTE_ROUTER port0
 */
public class GetPortRemoteRouterCommand extends RouterCommand {
    /**
     * Construct a GetPortRemoteRouterCommand
     */
    public GetPortRemoteRouterCommand() {
        super(MCRP.GET_PORT_REMOTE_ROUTER.CMD, MCRP.GET_PORT_REMOTE_ROUTER.CODE, MCRP.GET_PORT_REMOTE_ROUTER.ERROR);
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
            String rest = value.substring(MCRP.GET_PORT_REMOTE_ROUTER.CMD.length()).trim();
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

                    // get name on netIF in port
                    NetIF netIF = routerPort.getNetIF();
                    String name = netIF.getRemoteRouterName();

                    JSONObject jsobj = new JSONObject();

                    if (name != null) {
                        jsobj.put("name", name);
                        out.println(jsobj.toString());
                    } else {
                        jsobj.put("name", "");
                        out.println(jsobj.toString());
                    }

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