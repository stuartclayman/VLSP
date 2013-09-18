package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;
import usr.router.AppSocketMux;
import usr.router.NetIF;
import usr.router.NetStats;

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
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = new JSONObject();

            controller.listPorts();
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

        return false;
    }

}