package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import usr.console.Request;
/**
 * The SHUT_DOWN command.
 */
public class ShutDownCommand extends RouterCommand {
    /**
     * Construct a ShutDownCommand.
     */
    public ShutDownCommand() {
        super(MCRP.SHUT_DOWN.CMD, MCRP.SHUT_DOWN.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
       // it is an asynchronous command
        // and will be processed a bit later
        SocketChannel sc = getChannel();
        Logger.getLogger("log").logln(USR.STDOUT, "Shutdown command asynchronous");
        managementConsole.addRequest(new Request(sc, req));
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Requests = " + managementConsole.queue());
        return true;
    }

}
