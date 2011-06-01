package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
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
        if (args.length != 4 && args.length != 5) {
            error("Expected four or five arguments for New Router Command: NEW_ROUTER id mcrpPort r2rPort [name].");
            return false;
        }
        int rId,port1,port2;
        String name = null;

        try {
            rId= Integer.parseInt(args[1]);
            port1= Integer.parseInt(args[2]);
            port2= Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            error("Argument for new router command must be int");
            return false;
        }

        if (args.length == 5) {
            // there is a name too
            name = args[4];
        }

        String routerName = controller.requestNewRouter(rId,port1,port2, name);

        if (routerName != null) {
            success(routerName);  // WAS success("NEW ROUTER STARTED");
            return true;
        } else {
            error("CANNOT START NEW ROUTER");
            return false;
        }
    }

}
