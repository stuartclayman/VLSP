package usr.localcontroller.command;

import usr.protocol.MCRP;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The SHUT_DOWN command.
 */
public class ShutDownCommand extends LocalCommand {
    /**
     * Construct a ShutDownCommand.
     */
    public ShutDownCommand() {
        super(MCRP.SHUT_DOWN.CMD, MCRP.SHUT_DOWN.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        controller.shutDown();
        success("SHUTDOWN");
        return true;
    }

}
