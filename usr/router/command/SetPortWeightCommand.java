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
 * The SET_PORT_WEIGHT command.
 * SET_PORT_WEIGHT port weight
 * SET_PORT_WEIGHT port0 15
 */
public class SetPortWeightCommand extends RouterCommand {
    /**
     * Construct a SetPortWeightCommand.
     */
    public SetPortWeightCommand() {
        super(MCRP.SET_PORT_WEIGHT.CMD, MCRP.SET_PORT_WEIGHT.CODE, MCRP.SET_PORT_WEIGHT.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String rest = req.substring(MCRP.SET_PORT_WEIGHT.CMD.length()).trim();
        String[] parts = rest.split(" ");
                    
        if (parts.length == 2) {

            String routerPortName = parts[0];
            String weightStr = parts[1];
                        
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
            }

            // instantiate the weight
            int weight = Integer.MIN_VALUE;
            try {
                weight = Integer.parseInt(weightStr);
            } catch (NumberFormatException nfe) {
                error(getName() + " weight is not a number " + weightStr);
            }

            // set weight on netIF in port
            if (weight != Integer.MIN_VALUE) {
                NetIF netIF = routerPort.getNetIF();
                netIF.setWeight(weight);

                result = success(routerPortName);
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
