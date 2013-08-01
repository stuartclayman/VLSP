package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.Address;
import java.util.List;
import java.util.Map;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;


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

        finally {
            return false;
        }


    }

}