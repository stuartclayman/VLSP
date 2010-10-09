package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.console.Request;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The END_LINK command.
 */
public class EndLinkCommand extends RouterCommand {
    /**
     * Construct an EndLinkCommand
     */
    public EndLinkCommand() {
        super(MCRP.END_LINK.CMD, MCRP.END_LINK.CODE, 
          MCRP.END_LINK.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        // it is an asynchronous command
        // and will be processed a bit later
        SocketChannel sc = getChannel();

        managementConsole.addRequest(new Request(sc, req));
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Requests = " + managementConsole.queue());
        return true;

    }

}
