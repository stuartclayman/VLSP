package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The QUIT command.
 */
public class QuitCommand extends RouterCommand {
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
