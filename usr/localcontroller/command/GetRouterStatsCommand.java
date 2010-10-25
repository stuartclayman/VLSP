package usr.localcontroller.command;

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
public class GetRouterStatsCommand extends LocalCommand {
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
                // no router with that name
                error("No routers on this LocalController");
                return true;
            } 

        } else if (args.length == 2) {
            Scanner sc = new Scanner(args[1]);
            int routerID;

            if (sc.hasNextInt()) {
                routerID = sc.nextInt();
            } else {
                error("Argument for GET_ROUTER_STATS command must be int");
                return false;
            }

            // Get controller to do the work
            // and get stats for the router
            list = controller.getRouterStats(routerID);

            if (list == null) {
                // no router with that name
                error("No router with ID " + routerID + " on this LocalController");
                return true;
            } 

        } else {
            error("Expected 2 arguments GET_ROUTER_STATS [router_id]");
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

