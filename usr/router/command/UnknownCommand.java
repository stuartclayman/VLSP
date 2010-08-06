package usr.router.command;

import usr.router.Command;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The command to execute if the incoming command is unknown.
 */
public class UnknownCommand extends AbstractCommand {
    /**
     * Construct a UnknownCommand
     */
    public UnknownCommand() {
        super("__UNKNOWN__", 400, 400);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = error("UNKNOWN " + req);

        if (!result) {
            System.err.println("MC: UNKNOWN failed");
        }

        return result;
    }

}
