package usr.router.command;

import usr.router.Command;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The QUIT command.
 */
public class QuitCommand extends AbstractCommand {
    /**
     * Construct a QuitCommand.
     */
    public QuitCommand(int succCode, int errCode) {
        super("QUIT", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public void evaluate(String req) {
        try {
            success("BYE");
            managementConsole.endConnection(getChannel());
        } catch (IOException ioe) {
            System.err.println("MC: QUIT failed");
        }

    }

}
