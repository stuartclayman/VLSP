package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.Address;
import java.util.List;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The LIST_CONNECTIONS command.
 */
public class ListRoutingTableCommand extends RouterCommand {
    /**
     * Construct a ListConnectionsCommand.
     */
    public ListRoutingTableCommand() {
        super(MCRP.LIST_ROUTING_TABLE.CMD, MCRP.LIST_ROUTING_TABLE.CODE, 
          MCRP.LIST_ROUTING_TABLE.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
               
        list(controller.listRoutingTable());
        boolean result = success("END");

        if (!result) {
            System.err.println(leadin() + "LIST_CONNECTIONS response failed");
        }

        return result;

    }

}
