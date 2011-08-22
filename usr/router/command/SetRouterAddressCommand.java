package usr.router.command;

import usr.protocol.MCRP;
import usr.net.Address;
import usr.net.AddressFactory;
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

        boolean result;

        Address addr = null;
        try {
          addr= AddressFactory.newAddress(idStr);
        } catch (java.net.UnknownHostException e) {
          result= error("Cannot construct address from "+idStr);
          return result;
        }

        boolean idSet = controller.setAddress(addr);

        if (idSet) {
            result = success("" + idSet);
        } else {
            result = error("Cannot set Global Address after communication");
        }

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + getName() + " response failed");
        }

        return result;
    }

}
