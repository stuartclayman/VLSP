package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.protocol.MCRP;
import usr.router.NetIF;
import usr.router.RouterPort;


/**
 * The LIST_CONNECTIONS command.
 */
public class ListConnectionsCommand extends RouterCommand {
    /**
     * Construct a ListConnectionsCommand.
     */
    public ListConnectionsCommand() {
        super(MCRP.LIST_CONNECTIONS.CMD, MCRP.LIST_CONNECTIONS.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = new JSONObject();

            List<RouterPort> ports = controller.listPorts();
            int count = 0;

            for (RouterPort rp : ports) {
                if (rp.equals(RouterPort.EMPTY)) {
                    continue;
                } else {
                    NetIF netIF = rp.getNetIF();
                    Address address = netIF.getAddress();
                    Address remoteAddress = netIF.getRemoteRouterAddress();
                    int port = rp.getPortNo();

                    String portString = "port" + port + " " +
                        netIF.getName() + " W(" + netIF.getWeight() + ") = " +
                        controller.getName() + " " +
                        (address == null ? "No_Address" : address) +
                        " => " + netIF.getRemoteRouterName() + " " +
                        (remoteAddress == null ? "No_Remote_Address" : remoteAddress);

                    jsobj.put(Integer.toString(port), portString);

                    count++;
                }
            }

            jsobj.put("size", count);

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