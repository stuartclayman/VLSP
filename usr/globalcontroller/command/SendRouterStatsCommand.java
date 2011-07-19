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
 * The SendRouterStatsCommand command -- receives stats from local controller
 */
public class SendRouterStatsCommand extends GlobalCommand  {
    // the original request
    String request;

    /**
     * Construct a GetRouterStatsCommand.
     */
    public SendRouterStatsCommand() {
        super(MCRP.SEND_ROUTER_STATS.CMD, MCRP.SEND_ROUTER_STATS.CODE, 111);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        //request = req;
        //System.err.println("RECEIVED STATS FROM "+leadin()+" "+req);
        String rest = req.substring(MCRP.SEND_ROUTER_STATS.CMD.length()).trim();
        controller.receiveRouterStats(rest);
        return success("STATS RECEIVED");
    }
}

