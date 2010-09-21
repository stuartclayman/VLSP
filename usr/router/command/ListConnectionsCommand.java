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
public class ListConnectionsCommand extends RouterCommand {
    /**
     * Construct a ListConnectionsCommand.
     */
    public ListConnectionsCommand() {
        super(MCRP.LIST_CONNECTIONS.CMD, MCRP.LIST_CONNECTIONS.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        List<RouterPort> ports = controller.listPorts();
        int count = 0;
        for (RouterPort rp : ports) {
            if (rp.equals(RouterPort.EMPTY)) {
                continue;
            } else {
                NetIF netIF = rp.getNetIF();
                Address address = netIF.getAddress();
                Address remoteAddress = netIF.getRemoteRouterAddress();

                String portString = "port" + rp.getPortNo() + " " + 
                    netIF.getName() + " W(" + netIF.getWeight() + ") = " +
                    controller.getName() + " " + 
                    (address == null ? "No_Address" : "@(" + address + ")") +
                    " => " + netIF.getRemoteRouterName() + " " +
                    (remoteAddress == null ? "No_Remote_Address" : "@(" + remoteAddress + ")");


                list(portString);
                count++;
            }               
        }             

        boolean result = success("END " + count);

        if (!result) {
            System.err.println(leadin() + "LIST_CONNECTIONS response failed");
        }

        return result;

    }

}
