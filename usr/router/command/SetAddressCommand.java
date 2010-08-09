package usr.router.command;

import usr.router.*;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.UnknownHostException;

/**
 * The SET_ADDRESS command.
 * SET_ADDRESS port type address
 * SET_ADDRESS port0 IPV4 192.168.1.53
 */
public class SetAddressCommand extends AbstractCommand {
    /**
     * Construct a SetAddressCommand.
     */
    public SetAddressCommand() {
        super(MCRP.SET_ADDRESS.CMD, MCRP.SET_ADDRESS.CODE, MCRP.SET_ADDRESS.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String rest = req.substring(11).trim();
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
            } else {
                error(getName() + " unknown address type " + type);
            }

            // set address on netIF in port
            NetIF netIF = routerPort.getNetIF();
            netIF.setAddress(address);

            result = success(routerPortName);
        } else {
            error(getName() + " wrong no of args ");
        }

        if (!result) {
            System.err.println("MC: " + getName() + " failed");
        }

        return result;
    }

}
