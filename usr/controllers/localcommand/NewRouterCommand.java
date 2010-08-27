package usr.controllers.localcommand;

import usr.interactor.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The QUIT command.
 */
public class NewRouterCommand extends LocalCommand {
    /**
     * Construct a QuitCommand.
     */
    public NewRouterCommand() {
        super(MCRP.NEW_ROUTER.CMD, MCRP.NEW_ROUTER.CODE, MCRP.NEW_ROUTER.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String []args= req.split(" ");
        if (args.length != 2) {
            error("Expected two arguments for New Router Command");
            return false;
        }
        int rId;
        try {
            rId= Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            error("Argument for new router command must be int");
            return false;
        }
        if (managementConsole.requestNewRouter(rId)) {
            success("NEW ROUTER STARTED");
            return true;
        }
        error("CANNOT START NEW ROUTER");
        return false;
    }

}
