package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.UnknownHostException;

/**
 * The GET_PORT_NAME command.
 * GET_PORT_NAME port 
 * GET_PORT_NAME port0
 */
public class GetPortNameCommand extends RouterCommand {
    /**
     * Construct a GetPortNameCommand.
     */
    public GetPortNameCommand() {
        super(MCRP.GET_PORT_NAME.CMD, MCRP.GET_PORT_NAME.CODE, MCRP.GET_PORT_NAME.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String rest = req.substring(MCRP.GET_PORT_NAME.CMD.length()).trim();
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
            String name = netIF.getName();

            if (name != null) {
                result = success(name.toString());
            } else {
                result = success("");
            }

        } else {
            error(getName() + " wrong no of args ");
        }

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + getName() + " failed");
        }

        return result;
    }

}
