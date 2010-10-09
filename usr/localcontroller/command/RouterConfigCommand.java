package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * The ROUTER_CONFIG command takes a string for default router config
 */
public class RouterConfigCommand extends LocalCommand {
    /**
     * Construct a LocalCheckCommand.
     */
    public RouterConfigCommand() {
        super(MCRP.ROUTER_CONFIG.CMD, MCRP.ROUTER_CONFIG.CODE,
           MCRP.ROUTER_CONFIG.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        
        String rest = req.substring(MCRP.ROUTER_CONFIG.CMD.length()).trim();
        controller.setRouterOptions(rest);
        success("Router Config String received");
        return true;
    }

}
