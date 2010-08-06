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
    public void evaluate(String req) {
        try {
            int port = controller.getConnectionPort();
            success(""+port);
        } catch (IOException ioe) {
            System.err.println("MC: GET_CONNECTION_PORT failed");
        }
    }

}
