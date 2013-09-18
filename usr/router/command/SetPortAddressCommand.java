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
import usr.net.Address;
import usr.net.AddressFactory;
import usr.protocol.MCRP;
import usr.router.NetIF;
import usr.router.RouterPort;

/**
 * The SET_PORT_ADDRESS command.
 * SET_PORT_ADDRESS port  address
 * SET_PORT_ADDRESS port0 192.168.1.53
 */
public class SetPortAddressCommand extends RouterCommand {
    /**
     * Construct a SetPortAddressCommand.
     */
    public SetPortAddressCommand() {
        super(MCRP.SET_PORT_ADDRESS.CMD, MCRP.SET_PORT_ADDRESS.CODE, MCRP.SET_PORT_ADDRESS.ERROR);
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
            String rest = value.substring(MCRP.SET_PORT_ADDRESS.CMD.length()).trim();
            String[] parts = rest.split(" ");

            if (parts.length == 2) {

                String routerPortName = parts[0];
                String addr = parts[1];
                Address address = null;

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

                    // instantiate the address
                    try {
                        address = AddressFactory.newAddress(addr);
                    } catch (Exception e) {
                        response.setCode(302);

                        JSONObject jsobj = new JSONObject();
                        jsobj.put("error", getName() + " address error " + e);
                        out.println(jsobj.toString());
                        response.close();

                        return false;
                    }

                    // set address on netIF in port
                    if (address != null) {
                        NetIF netIF = routerPort.getNetIF();
                        netIF.setAddress(address);

                        JSONObject jsobj = new JSONObject();

                        jsobj.put("name", routerPortName);
                        jsobj.put("address", address.toString());
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

        return false;


    }

}