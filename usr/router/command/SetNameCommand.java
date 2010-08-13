package usr.router.command;

import usr.interactor.Command;
import usr.router.RouterManagementConsole;
import usr.router.MCRP;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The SET_NAME command.
 * SET_NAME name
 * SET_NAME Router-47
 */
public class SetNameCommand extends AbstractCommand {
    /**
     * Construct a SetNameCommand.
     */
    public SetNameCommand() {
        super(MCRP.SET_NAME.CMD, MCRP.SET_NAME.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String name = req.substring(8).trim();
        controller.setName(name);

        boolean result = success(name);

        if (!result) {
            System.err.println("MC: " + getName() + " response failed");
        }

        return result;
    }

}
