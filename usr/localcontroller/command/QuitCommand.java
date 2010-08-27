package usr.localcontroller.command;

import usr.console.MCRP;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The QUIT command.
 */
public class QuitCommand extends LocalCommand {
    /**
     * Construct a QuitCommand.
     */
    public QuitCommand() {
        super(MCRP.QUIT.CMD, MCRP.QUIT.CODE, MCRP.ERROR.CODE);
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
