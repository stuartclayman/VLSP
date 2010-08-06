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
    public boolean evaluate(String req) {
        success("BYE");
        managementConsole.endConnection(getChannel());

        return true;
    }

}
