package usr.router.command;

import usr.router.*;
import usr.net.Address;
import java.util.List;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The LIST_CONNECTIONS command.
 */
public class ListConnectionsCommand extends AbstractCommand {
    /**
     * Construct a ListConnectionsCommand.
     */
    public ListConnectionsCommand(int succCode, int errCode) {
        super("LIST_CONNECTIONS", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public void evaluate(String req) {
        try {
            List<RouterPort> ports = controller.listPorts();
            success("START");
            int count = 0;
            for (RouterPort rp : ports) {
                if (rp.equals(RouterPort.EMPTY)) {
                    continue;
                } else {
                    NetIF netIF = rp.getNetIF();
                    Address address = netIF.getAddress();
                    String portString = " port" + rp.getPortNo() + " " + netIF.getName() + " " + netIF.getRemoteRouterName() + " " + netIF.getWeight() + " " + (address == null ? "No_Address" : address.toString());
                    success(count + portString);
                    count++;
                }               
            }             
            success("END");
        } catch (IOException ioe) {
            System.err.println("MC: LIST_CONNECTIONS failed");
        }

    }

}
