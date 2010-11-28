package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.net.Address;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_ROUTER_ADDRESS command.
 */
public class GetRouterAddressCommand extends RouterCommand {
    /**
     * Construct a GetRouterAddressCommand
     */
    public GetRouterAddressCommand() {
        super(MCRP.GET_ROUTER_ADDRESS.CMD, MCRP.GET_ROUTER_ADDRESS.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        Address a = controller.getAddress();
        int id= a.asInteger();
        boolean result = success("" + id);

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " GET_ROUTER_ADDRESS response failed");
        }

        return result;
    }

}
