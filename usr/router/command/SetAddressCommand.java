package usr.router.command;

import usr.router.*;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.UnknownHostException;

/**
 * The SET_ADDRESS command.
 */
public class SetAddressCommand extends AbstractCommand {
    /**
     * Construct a SetAddressCommand.
     */
    public SetAddressCommand(int succCode, int errCode) {
        super("SET_ADDRESS", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public void evaluate(String req) {
        try {
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
                    error("SET_ADDRESS invalid port " + routerPortName);
                }

                // instantiate the address
                if (type.toUpperCase().equals("IPV4")) {
                    try {
                        address = new IPV4Address(addr);
                    } catch (UnknownHostException uhe) {
                        error("SET_ADDRESS UnknownHostException " + addr);
                    }
                } else {
                    error("SET_ADDRESS unknown type " + type);
                }

                // set address on netIF in port
                NetIF netIF = routerPort.getNetIF();
                netIF.setAddress(address);

                success(routerPortName);
            } else {
                error("SET_ADDRESS wrong no of args ");
            }
        } catch (IOException ioe) {
            System.err.println("MC: SET_ADDRESS failed");
        }
    }

}
