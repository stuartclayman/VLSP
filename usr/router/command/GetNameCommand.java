package usr.router.command;

import usr.interactor.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_NAME command.
 */
public class GetNameCommand extends RouterCommand {
    /**
     * Construct a GetNameCommand.
     */
    public GetNameCommand() {
        super(MCRP.GET_NAME.CMD, MCRP.GET_NAME.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String name = controller.getName();
        
        boolean result = success(name);

        if (!result) {
            System.err.println("MC: " + getName() + " response failed");
        }

        return result;
    }

}
