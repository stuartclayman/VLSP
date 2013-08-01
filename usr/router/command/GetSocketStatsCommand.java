package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.router.NetStats;
import usr.router.AppSocketMux;
import usr.net.Address;
import java.util.List;
import java.util.Map;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

/**
 * The GET_SOCKET_STATS command.
 */
public class GetSocketStatsCommand extends RouterCommand {
    /**
     * Construct a GetSocketStatsCommand.
     */
    public GetSocketStatsCommand() {
        super(MCRP.GET_SOCKET_STATS.CMD, MCRP.GET_SOCKET_STATS.CODE, MCRP.ERROR.CODE);
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

            // get local NetIF
            NetIF netIF = controller.getLocalNetIF();

            if (netIF == null) {
                jsobj.put("END", "0");

                out.println(jsobj.toString());
                response.close();

                return true;

            } else {

                // double check it is an AppSocketMux
                if (netIF instanceof AppSocketMux) {
                    // get stats for sockets
                    Map<Integer, NetStats> socketStats = ((AppSocketMux)netIF).getSocketStats();

                    // now list the stats, one line per socket
                    for (int port : socketStats.keySet()) {
                        NetStats stats = socketStats.get(port);

                        if (stats == null) {
                            continue;
                        }

                        // put out netif name
                        String statsString = port + " " + stats.toString();

                        jsobj.put(Integer.toString(port), statsString);
                        count++;

                    }

                }

                jsobj.put("size", count);

                out.println(jsobj.toString());
                response.close();

                return true;

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