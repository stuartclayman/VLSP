package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.common.LocalHostInfo;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;

/**
 * The GetRouterStatsCommand command.
 */
public class GetRouterStatsCommand extends GlobalCommand {
    /**
     * Construct a GetRouterStatsCommand.
     */
    public GetRouterStatsCommand() {
        super(MCRP.GET_ROUTER_STATS.CMD, MCRP.GET_ROUTER_STATS.CODE, MCRP.ERROR.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        // the result list
        List<String> list;

        // get arg for specified router
        String []args= req.split(" ");

        if (args.length == 1) {
            // no args so get data for all routers

            // Get controller to do the work
            // and get stats for the router
            list = controller.getRouterStats();

            if (list == null) {
                // no routers
                error("No routers on this GlobalController");
                return true;
            } 

        } else {
            error("Expected GET_ROUTER_STATS");
            return false;
        }



        // now return the list
        int size = list.size();

        for (String stat : list) {
            list(stat);
        }

        boolean result = success("END " + size);

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "LIST_CONNECTIONS response failed");
        }

        return result;

    }
}

