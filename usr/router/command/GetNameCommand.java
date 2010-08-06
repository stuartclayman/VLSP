package usr.router.command;

import usr.router.Command;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_NAME command.
 */
public class GetNameCommand extends AbstractCommand {
    /**
     * Construct a GetNameCommand.
     */
    public GetNameCommand(int succCode, int errCode) {
        super("GET_NAME", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public void evaluate(String req) {
        try {
            String name = controller.getName();
            success(name);
        } catch (IOException ioe) {
            System.err.println("MC: GET_NAME failed");
        }
    }

}
