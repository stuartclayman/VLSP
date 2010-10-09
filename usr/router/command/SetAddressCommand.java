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
 * SET_PORT_ADDRESS port type address
 * SET_PORT_ADDRESS port0 IPV4 192.168.1.53
 */
public class SetAddressCommand extends RouterCommand {
    /**
     * Construct a SetAddressCommand.
     */
    public SetAddressCommand() {
        super(MCRP.SET_PORT_ADDRESS.CMD, MCRP.SET_PORT_ADDRESS.CODE, MCRP.SET_PORT_ADDRESS.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String rest = req.substring(MCRP.SET_PORT_ADDRESS.CMD.length()).trim();
        String[] parts = rest.split(" ");
                    
        if (parts.length == 3) {

            String routerPortName = parts[0];
            String type = parts[1];
            String addr = parts[2];
            Address address = null;
                        
            // find port
            String portNo = routerPortName.substring(4);
            Scanner scanner = new Scanner(portNo);
            int p = scanner.nextInt();
            RouterPort routerPort = controller.getPort(p);

            if (routerPort == null || routerPort == RouterPort.EMPTY) {
                error(getName() + " invalid port " + routerPortName);
            }

            // instantiate the address
            if (type.toUpperCase().equals("IPV4")) {
                try {
                    address = new IPV4Address(addr);
                } catch (UnknownHostException uhe) {
                    error(getName() + " UnknownHostException " + addr);
                }

            } else if (type.toUpperCase().equals("GID")) {
                Scanner addrScan = new Scanner(addr);

                if (addrScan.hasNextInt()) {
                    int gid = addrScan.nextInt();
                    address = new GIDAddress(gid);
                } else {
                    error(getName() + " Illegal GID address " + addr);
                }

            } else {
                error(getName() + " unknown address type " + type);
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
