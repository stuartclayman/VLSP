package usr.controllers.localcommand;

import usr.interactor.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The QUIT command.
 */
public class LocalCheckCommand extends AbstractCommand {
    /**
     * Construct a QuitCommand.
     */
    public LocalCheckCommand() {
        super(MCRP.CHECK_LOCAL_CONTROLLER.CMD, MCRP.CHECK_LOCAL_CONTROLLER.CODE, 
          MCRP.CHECK_LOCAL_CONTROLLER.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        success("Ping from local controller");
        managementConsole.contactFromGlobal(getChannel());

        return true;
    }

}
