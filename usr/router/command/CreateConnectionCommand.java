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
     */
    public CreateConnectionCommand(int succCode, int errCode) {
        super("CREATE_CONNECTION", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public void evaluate(String req) {
        // it is an asynchronous command
        // and will be processed a bit later
        SocketChannel sc = getChannel();

        managementConsole.addRequest(new Request(sc, req));
        System.err.println("MC: Requests = " + managementConsole.queue());

    }

}
