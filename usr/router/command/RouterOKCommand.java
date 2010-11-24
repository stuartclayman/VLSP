package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.UnknownHostException;

/**
 * The Router OK command simply checks a router is functioning
 */
public class RouterOKCommand extends RouterCommand {
    /**
     * Constructor
     */
    public RouterOKCommand() {
        super(MCRP.ROUTER_OK.CMD, MCRP.ROUTER_OK.CODE, MCRP.ERROR.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        success("OK");
        return true;
    }

}
