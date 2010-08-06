package usr.router.command;

import usr.router.Command;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The SET_NAME command.
 */
public class SetNameCommand extends AbstractCommand {
    /**
     * Construct a SetNameCommand.
     */
    public SetNameCommand(int succCode, int errCode) {
        super("SET_NAME", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public void evaluate(String req) {
        try {
            String name = req.substring(8).trim();
            controller.setName(name);
            success(name);
        } catch (IOException ioe) {
            System.err.println("MC: SET_NAME failed");
        }
    }

}
