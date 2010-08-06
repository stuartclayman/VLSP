package usr.router.command;

import usr.router.Command;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_CONNECTION_PORT command.
 */
public class GetConnectionPortCommand extends AbstractCommand {
    /**
     * Construct a GetConnectionPortCommand
     */
    public GetConnectionPortCommand(int succCode, int errCode) {
        super("GET_CONNECTION_PORT", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        int port = controller.getConnectionPort();
        
        boolean result = success(""+port);

        if (!result) {
            System.err.println("MC: GET_CONNECTION_PORT failed");
        }

        return result;
    }

}
