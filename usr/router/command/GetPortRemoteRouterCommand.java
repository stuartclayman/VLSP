package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.UnknownHostException;

/**
 * The GET_PORT_REMOTE_ROUTER command.
 * GET_PORT_REMOTE_ROUTER port 
 * GET_PORT_REMOTE_ROUTER port0
 */
public class GetPortRemoteRouterCommand extends RouterCommand {
    /**
     * Construct a GetPortRemoteRouterCommand
     */
    public GetPortRemoteRouterCommand() {
        super(MCRP.GET_PORT_REMOTE_ROUTER.CMD, MCRP.GET_PORT_REMOTE_ROUTER.CODE, MCRP.GET_PORT_REMOTE_ROUTER.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String rest = req.substring(MCRP.GET_PORT_REMOTE_ROUTER.CMD.length()).trim();
        String[] parts = rest.split(" ");
                    
        if (parts.length == 1) {

            String routerPortName = parts[0];
                        
            // find port
            String portNo = routerPortName.substring(4);
            Scanner scanner = new Scanner(portNo);
            int p = scanner.nextInt();
            RouterPort routerPort = controller.getPort(p);

            if (routerPort == null || routerPort == RouterPort.EMPTY) {
                error(getName() + " invalid port " + routerPortName);
            }

            // get name on netIF in port
            NetIF netIF = routerPort.getNetIF();
            String name = netIF.getRemoteRouterName();

            if (name != null) {
                result = success(name.toString());
            } else {
                result = success("");
            }

        } else {
            error(getName() + " wrong no of args ");
        }

        if (!result) {
            System.err.println(leadin() + getName() + " failed");
        }

        return result;
    }

}
