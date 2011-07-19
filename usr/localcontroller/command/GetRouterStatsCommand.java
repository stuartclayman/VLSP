package usr.localcontroller.command;

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
public class GetRouterStatsCommand extends LocalCommand implements Callable<Boolean> {
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
	    Logger.getLogger("log").logln(USR.ERROR, leadin() + "GET_ROUTER_STATS response failed");
	}

	return result;

    }
}

