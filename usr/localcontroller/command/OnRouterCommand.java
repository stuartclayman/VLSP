package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import java.util.Scanner;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The ON_ROUTER command.
 */
public class OnRouterCommand extends LocalCommand {
    /**
     * Construct a OnRouterCommand.
     */
    public OnRouterCommand() {
        super(MCRP.ON_ROUTER.CMD, MCRP.ON_ROUTER.CODE, MCRP.ERROR.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String []args= req.split(" ");
        if (args.length < 4) {
            error("Expected three or more arguments for ON_ROUTER Command: ON_ROUTER router_id className args");
            return false;
        } else {
            Scanner sc = new Scanner(args[1]);
            int routerID;

            if (sc.hasNextInt()) {
                routerID = sc.nextInt();
            } else {
                error("Argument for ON_ROUTER command must be int");
                return false;
            }

            String className = args[2];

            // collect args
            String[] cmdArgs = new String[args.length - 3];

            for (int a=3; a < args.length; a++) {
                cmdArgs[a-3] = args[a];
            }
                

            // get controller to do work
            String result = controller.onRouter(routerID, className, cmdArgs);

            if (result != null) {
                success(result);
                return true;
            } else {
                error("ON_ROUTER. ERROR with " + req);
                return false;
            }
        }
    }
}
