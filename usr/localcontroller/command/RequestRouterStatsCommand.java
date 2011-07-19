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
public class RequestRouterStatsCommand extends LocalCommand {
    // the original request
    String request;

    /**
     * Construct a GetRouterStatsCommand.
     */
    public RequestRouterStatsCommand() {
        super(MCRP.REQUEST_ROUTER_STATS.CMD, MCRP.REQUEST_ROUTER_STATS.CODE, MCRP.ERROR.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        if (controller.getGlobalControllerInteractor() == null) {
            error("RequestRouterStatsCommand: No global controller present");
            return false;
        }

        success("REQUEST FOR STATS RECEIVED");
        List<String> list= controller.getRouterStats();
        return controller.sendRouterStats(list);
    }

}

