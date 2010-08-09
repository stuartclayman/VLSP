package usr.router.command;

import usr.router.Command;
import usr.router.MCRP;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_NAME command.
 */
public class GetNameCommand extends AbstractCommand {
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
