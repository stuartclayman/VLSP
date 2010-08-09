package usr.router.command;

import usr.router.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The CREATE_CONNECTION command.
 */
public class CreateConnectionCommand extends AbstractCommand {
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
        System.err.println("MC: Requests = " + managementConsole.queue());

        return true;
    }

}
