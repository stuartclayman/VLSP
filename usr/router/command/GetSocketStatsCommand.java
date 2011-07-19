package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.router.NetStats;
import usr.router.AppSocketMux;
import usr.net.Address;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.channels.SocketChannel;

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
    public boolean evaluate(String req) {
        List<RouterPort> ports = controller.listPorts();
        int count = 0;

        // get local NetIF
        NetIF netIF = controller.getLocalNetIF();
        if (netIF == null) {
            boolean result= success("END 0");
            return result;
        }

        // double check it is an AppSocketMux
        if (netIF instanceof AppSocketMux) {
            // get stats for sockets
            Map<Integer, NetStats> socketStats =  ((AppSocketMux)netIF).getSocketStats();

            // now list the stats, one line per socket
            for (int port : socketStats.keySet()) {
                NetStats stats = socketStats.get(port);
                if (stats == null)
                    continue;

                // put out netif name
                String statsString = port + " " + stats.toString();

                list(statsString);
                count++;

            }

        }



        boolean result = success("END " + count);

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "LIST_CONNECTIONS response failed");
        }

        return result;

    }

}
