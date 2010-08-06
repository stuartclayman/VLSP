package usr.router.command;

import usr.router.Command;
import usr.router.NetIF;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.*;
import java.util.Scanner;
import java.nio.channels.*;
import java.nio.*;


/**
 * The INCOMING_CONNECTION command.
 */
public class IncomingConnectionCommand extends AbstractCommand {
    /**
     * Construct a IncomingConnectionCommand
     */
    public IncomingConnectionCommand(int succCode, int errCode) {
        super("INCOMING_CONNECTION", succCode, errCode);
    }

    /**
     * Evaluate the Command.
     */
    public void evaluate(String req) {
        SocketChannel sc = getChannel();

        try {
            String args = req.substring(19).trim();
            String[] parts = args.split(" ");

            if (parts.length == 4) {

                String connectionID = parts[0];
                String remoteRouterName = parts[1];
                String weightStr = parts[2];
                String remotePort = parts[3];

                Scanner scanner;

                // get remote port
                scanner = new Scanner(remotePort);
                int port;

                try {
                    port = scanner.nextInt();
                } catch (Exception e) {
                    error("INCOMING_CONNECTION bad port number");
                    return;
                }

                // get connection weight
                scanner = new Scanner(weightStr);
                int weight = 0;

                try {
                    weight = scanner.nextInt();
                } catch (Exception e) {
                    error("INCOMING_CONNECTION invalid value for weight");
                    return;
                }


                InetSocketAddress refAddr = new InetSocketAddress(sc.socket().getInetAddress(), port);
                //System.err.println("ManagementConsole => " + refAddr + " # " + refAddr.hashCode());

                /*
                 * Lookup netif and set its name
                 */
                NetIF netIF = controller.getNetIFByID(refAddr.hashCode());

                if (netIF != null) {
                    System.err.println("MC: Found NetIF " + netIF + " by id " + refAddr.hashCode());

                    // set its name
                    netIF.setName(connectionID);
                    // set its weight
                    netIF.setWeight(weight);
                    // set remote router
                    netIF.setRemoteRouterName(remoteRouterName);
                        
                    // now plug netIF into Router
                    controller.plugInNetIF(netIF);

                    success("" +  connectionID);
                } else {
                    error("Cannot find NetIF for port " + port);
                }
            } else {
                error("INCOMING_CONNECTION wrong no of args ");
            }
        } catch (IOException ioe) {
            System.err.println("MC: INCOMING_CONNECTION failed");
        }
    }

}
