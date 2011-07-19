package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.common.LocalHostInfo;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * The GetRouterStatsCommand command.
 */
public class GetRouterStatsCommand extends GlobalCommand implements Callable<Boolean> {
    // the original request
    String request;

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
        request = req;

        // create an Executor pool
        ExecutorService pool = Executors.newCachedThreadPool();

        // run GET_ROUTER_STATS collection in separate thread
        Future<Boolean> future = pool.submit(this);

        // wait for result
        boolean result = false;

        try {
            result = future.get(); // use future
        } catch (ExecutionException ex) {
            error("GetRouterStatsCommand: ExecutionException " + ex);
        } catch (InterruptedException ie) {
            error("GetRouterStatsCommand: InterruptedException " + ie);
        }

        // shutdown pool
        pool.shutdown();

        return result;
    }

    public Boolean call() {
        // the result list
        List<String> list;

        // get arg for specified router
        String [] args= request.split(" ");

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
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "GET_ROUTER_STATS response failed");
        }

        return result;

    }
}

