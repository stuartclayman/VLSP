package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import usr.router.NetIF;
import usr.net.GIDAddress;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.*;
import java.util.Scanner;
import java.nio.channels.*;
import java.nio.*;


/**
 * The INCOMING_CONNECTION command.
 * INCOMING_CONNECTION connectionID routerName routerID weight TCP-port
 * INCOMING_CONNECTION /Router283836798/Connection-1 Router283836798 4132 20 57352
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

        if (parts.length == 5) {

            String connectionID = parts[0];
            String remoteRouterName = parts[1];
            String remoteRouterID = parts[2];
            String weightStr = parts[3];
            String remotePort = parts[4];

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

            // get remote address
            scanner = new Scanner(remoteRouterID);
            int remoteID;

            try {
                remoteID = scanner.nextInt();
            } catch (Exception e) {
                error(getName() + " invalid value for routerID");
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
            //Logger.getLogger("log").logln(USR.ERROR, "ManagementConsole => " + refAddr + " # " + refAddr.hashCode());

            /*
             * Lookup netif and set its name
             */
            NetIF netIF = controller.getTemporaryNetIFByID(refAddr.hashCode());

            if (netIF != null) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Found temporary NetIF with id " + refAddr.hashCode());

                // set its name
                netIF.setName(connectionID);
                // set its weight
                netIF.setWeight(weight);
                // set its Address
                netIF.setAddress(controller.getAddress()); // WAS new GIDAddress(controller.getGlobalID()));
                // set remote router
                netIF.setRemoteRouterName(remoteRouterName);
                netIF.setRemoteRouterAddress(new GIDAddress(remoteID));
                        
                // now plug netIF into Router
                controller.plugTemporaryNetIFIntoPort(netIF);

                result = success("" +  connectionID);
            } else {
                error("Cannot find NetIF for port " + port);
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
