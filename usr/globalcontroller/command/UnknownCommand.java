package usr.globalcontroller.command;

import usr.protocol.MCRP;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The command to execute if the incoming command is unknown.
 */
public class UnknownCommand extends GlobalCommand {
    /**
     * Construct a UnknownCommand
     */
    public UnknownCommand() {
        super("__UNKNOWN__", MCRP.ERROR.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = error("UNKNOWN " + req);

        if (!result) {
            System.err.println(leadin() + "UNKNOWN failed");
        }

        return result;
    }

}
