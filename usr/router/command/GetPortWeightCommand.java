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
 * The GET_PORT_WEIGHT command.
 * GET_PORT_WEIGHT port
 * GET_PORT_WEIGHT port0
 */
public class GetPortWeightCommand extends RouterCommand {
    /**
     * Construct a GetPortWeightCommand.
     */
    public GetPortWeightCommand() {
        super(MCRP.GET_PORT_WEIGHT.CMD, MCRP.GET_PORT_WEIGHT.CODE, MCRP.GET_PORT_WEIGHT.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String rest = req.substring(MCRP.GET_PORT_WEIGHT.CMD.length()).trim();
        String[] parts = rest.split(" ");

        if (parts.length == 1) {

            String routerPortName = parts[0];

            // find port
            String portNo;

            if (routerPortName.startsWith("port")) {
                portNo = routerPortName.substring(4);
            } else {
                portNo = routerPortName;
            }

            Scanner scanner = new Scanner(portNo);
            int p = scanner.nextInt();
            RouterPort routerPort = controller.getPort(p);

            if (routerPort == null || routerPort == RouterPort.EMPTY) {
                error(getName() + " invalid port " + routerPortName);
                return false;
            }

            // get weight on netIF in port
            NetIF netIF = routerPort.getNetIF();
            int weight = netIF.getWeight();

            result = success(Integer.toString(weight));


        } else {
            error(getName() + " wrong no of args ");
        }

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + getName() + " failed");
        }

        return result;
    }

}
