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
 * The SET_PORT_ADDRESS command.
 * SET_PORT_ADDRESS port  address
 * SET_PORT_ADDRESS port0 192.168.1.53
 */
public class SetPortAddressCommand extends RouterCommand {
    /**
     * Construct a SetPortAddressCommand.
     */
    public SetPortAddressCommand() {
        super(MCRP.SET_PORT_ADDRESS.CMD, MCRP.SET_PORT_ADDRESS.CODE, MCRP.SET_PORT_ADDRESS.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String rest = req.substring(MCRP.SET_PORT_ADDRESS.CMD.length()).trim();
        String[] parts = rest.split(" ");

        if (parts.length == 2) {

            String routerPortName = parts[0];
            String addr = parts[1];
            Address address = null;

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

            // instantiate the address
            try {
                address = AddressFactory.newAddress(addr);
            } catch (Exception e) {
                error(getName() + " address error " + e);
            }

            // set address on netIF in port
            if (address != null) {
                NetIF netIF = routerPort.getNetIF();
                netIF.setAddress(address);

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
