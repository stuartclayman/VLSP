package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The SET_NAME command.
 * SET_NAME name
 * SET_NAME Router-47
 */
public class SetNameCommand extends RouterCommand {
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
        String name = req.substring(MCRP.SET_NAME.CMD.length()).trim();
        boolean nameSet = controller.setName(name);

        boolean result;

        if (nameSet) {
            result = success(name);
        } else {
            result = error("Cannot set name after communication");
        }

        if (!result) {
            System.err.println(leadin() + getName() + " response failed");
        }

        return result;
    }

}
