package usr.router.command;

import usr.protocol.MCRP;
import usr.net.GIDAddress;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

/**
 * The SET_ROUTER_ADDRESS command.
 * SET_ROUTER_ADDRESS address
 * SET_ROUTER_ADDRESS 47
 */
public class SetRouterAddressCommand extends RouterCommand {
    /**
     * Construct a SetRouterAddressCommand
     */
    public SetRouterAddressCommand() {
        super(MCRP.SET_ROUTER_ADDRESS.CMD, MCRP.SET_ROUTER_ADDRESS.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String idStr = req.substring(MCRP.SET_ROUTER_ADDRESS.CMD.length()).trim();

        Scanner scanner = new Scanner(idStr);

        boolean result;

        if (scanner.hasNextInt()) {
            int id = scanner.nextInt();
            boolean idSet = controller.setAddress(new GIDAddress(id));

            if (idSet) {
                result = success("" + id);
            } else {
                result = error("Cannot set Global Address after communication");
            }
        } else {
            result = error("Cannot set Global Address with value " + idStr);
        }

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + getName() + " response failed");
        }

        return result;
    }

}
