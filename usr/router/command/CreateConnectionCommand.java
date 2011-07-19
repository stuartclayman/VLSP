package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.console.Request;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The CREATE_CONNECTION command.
 */
public class CreateConnectionCommand extends RouterCommand {
    /**
     * Construct a CreateConnectionCommand.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a
     * connection weight of connection_weight
     */
    public CreateConnectionCommand() {
        super(MCRP.CREATE_CONNECTION.CMD, MCRP.CREATE_CONNECTION.CODE, MCRP.CREATE_CONNECTION.ERROR);
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
