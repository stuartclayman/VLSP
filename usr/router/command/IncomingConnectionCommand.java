package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import usr.router.NetIF;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.*;
import java.util.Scanner;
import java.nio.channels.*;
import java.nio.*;


/**
 * The INCOMING_CONNECTION command.
 * INCOMING_CONNECTION connectionID routerName weight port
 * INCOMING_CONNECTION /Router-Router283836798/Connection-1 Router283836798 20 57352
 */
public class IncomingConnectionCommand extends RouterCommand {
    /**
     * Construct a IncomingConnectionCommand
     */
    public IncomingConnectionCommand() {
        super(MCRP.INCOMING_CONNECTION.CMD, MCRP.INCOMING_CONNECTION.CODE, MCRP.INCOMING_CONNECTION.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        SocketChannel sc = getChannel();

        boolean result = true;

        String args = req.substring(MCRP.INCOMING_CONNECTION.CMD.length()).trim();
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
                error(getName() + " bad port number");
                return true;
            }

            // get connection weight
            scanner = new Scanner(weightStr);
            int weight = 0;

            try {
                weight = scanner.nextInt();
            } catch (Exception e) {
                error(getName() + " invalid value for weight");
                return true;
            }

            // create an address from the same host, but
            // using the passed in port number
            InetSocketAddress refAddr = new InetSocketAddress(sc.socket().getInetAddress(), port);
            //System.err.println("ManagementConsole => " + refAddr + " # " + refAddr.hashCode());

            /*
             * Lookup netif and set its name
             */
            NetIF netIF = controller.getNetIFByID(refAddr.hashCode());

            if (netIF != null) {
                System.out.println(leadin() + "Found NetIF " + netIF + " by id " + refAddr.hashCode());

                // set its name
                netIF.setName(connectionID);
                // set its weight
                netIF.setWeight(weight);
                // set remote router
                netIF.setRemoteRouterName(remoteRouterName);
                        
                // now plug netIF into Router
                controller.plugInNetIF(netIF);

                result = success("" +  connectionID);
            } else {
                error("Cannot find NetIF for port " + port);
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
