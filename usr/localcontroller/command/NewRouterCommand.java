package usr.localcontroller.command;

import usr.protocol.MCRP;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The QUIT command.
 */
public class NewRouterCommand extends LocalCommand {
    /**
     * Construct a NewRouterCommand.
     */
    public NewRouterCommand() {
        super(MCRP.NEW_ROUTER.CMD, MCRP.NEW_ROUTER.CODE, MCRP.NEW_ROUTER.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String []args= req.split(" ");
        if (args.length != 4) {
            error("Expected four arguments for New Router Command");
            return false;
        }
        int rId,port1,port2;
        try {
            rId= Integer.parseInt(args[1]);
            port1= Integer.parseInt(args[2]);
            port2= Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            error("Argument for new router command must be int");
            return false;
        }
        if (managementConsole.requestNewRouter(rId,port1,port2)) {
            success("NEW ROUTER STARTED");
            return true;
        }
        error("CANNOT START NEW ROUTER");
        return false;
    }

}
