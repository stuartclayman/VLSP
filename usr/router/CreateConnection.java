package usr.router;

import usr.protocol.MCRP;
import usr.interactor.RouterInteractor;
import usr.interactor.MCRPException;
import usr.console.*;
import usr.net.*;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;


/**
 * A CreateConnection object, creates a connection from one router
 * to another.
 */
public class CreateConnection extends ChannelResponder implements Runnable {
    RouterController controller;
    Request request;

    /**
     * Create a new connection.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a 
     * connection weight of connection_weight
     */
    public CreateConnection(RouterController controller, Request request) {
        this.controller = controller;
        this.request = request;
        setChannel(request.channel);
    }

    public void run() {
        // process the request
        String value = request.value;
        SocketChannel channel = request.channel;

        // check command
        String[] parts = value.split(" ");
        if (parts.length != 3) {
            System.err.println(leadin() + "INVALID createConnection command: " + request);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION wrong no of args");
            return;
        }

        // check ip addr spec
        String[] ipParts = parts[1].split(":");
        if (ipParts.length != 2) {
            System.err.println(leadin() + "INVALID createConnection ip address: " + request);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION invalid address " + parts[1]);
            return;
        }

        // process host and port
        String host = ipParts[0];
        Scanner sc = new Scanner(ipParts[1]);
        int portNumber;

        try {
            portNumber = sc.nextInt();
        } catch (Exception e) {
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION invalid port " + ipParts[1]);
            return;
        }

        // get weight/distance factor
        sc = new Scanner(parts[2]);
        int weight;

        try {
            weight = sc.nextInt();
        }  catch (Exception e) {
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION invalid weight " + parts[2]);
            return;
        }

        // if we get here all the args seem OK

        /*
         * Connect to management port of remote router.
         */

        // Create a RouterInteractor to the remote Router
        RouterInteractor interactor = null;
        String routerResponse;


        try {
            interactor = new RouterInteractor(InetAddress.getByName(host), portNumber);

        } catch (UnknownHostException uhe) {
            System.err.println(leadin() + "Unknown host: " + host);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Unknown host: " + host);
            return;
        } catch (IOException ioexc) {
            System.err.println(leadin() + "Cannot connect to " + host + " on port " + portNumber + " -> " + ioexc);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot interact with host: " + host + " on port " + portNumber);
            return;
        }




        // at this point we have a connection to 
        // the managementSocket of the remote router

        try {
            routerResponse = interactor.getConnectionPort();
        } catch (IOException ioexc) {
            System.err.println(leadin() + "Cannot GET_CONNECTION_PORT from " + host + " -> " + ioexc);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot GET_CONNECTION_PORT from host: " + host);
            return;
        } catch (MCRPException mcrpe) {
            System.err.println(leadin() + "Cannot GET_CONNECTION_PORT from " + host + " -> " + mcrpe);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot GET_CONNECTION_PORT from host: " + host);
            return;
        }

        // Ok now we need to find the port the remote router
        // listens to for connections
        Scanner scanner = new Scanner(routerResponse);

        // now get connection port
        int connectionPort = scanner.nextInt();

        System.err.println(leadin() + "createConnection: connectionPort at " + host + " is " + connectionPort);


        /*
         * Now connect to connections port of remote router.
         */

        // make a socket connection for the router - to - router path
        Socket socket = null;
        NetIF netIF = null;
        InetSocketAddress refAddr;
        String latestConnectionId = "/Router-" + controller.getName() + "/Connection-" + controller.getConnectionCount();

        try {
	    // make a socket connection to a remote router

            TCPEndPointSrc src = new TCPEndPointSrc(host, connectionPort);
            Connection connection = new  ConnectionOverTCP(src);
            connection.connect();

            socket = src.getSocket();

            System.err.println(leadin() + "connection socket: " + socket);

            refAddr = new InetSocketAddress(socket.getInetAddress(), socket.getLocalPort());;
            //System.err.println("CreateConnection => " + refAddr + " # " + refAddr.hashCode());

            // wrap EndPoint as a NetIF

            netIF = new TCPNetIF(src);
            netIF.setName(latestConnectionId);
            netIF.setWeight(weight);
            netIF.setID(refAddr.hashCode());

            System.out.println(leadin() + "netif = " + netIF);


        } catch (UnknownHostException uhe) {
            System.err.println(leadin() + "Unknown host: " + host);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Unknown host: " + host);
            return;
        } catch (IOException ioexc) {
            System.err.println(leadin() + "Cannot connect to " + host + " on port " + connectionPort + " -> " + ioexc);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot interact with host: " + host + " on port " + connectionPort);
            return;
        }

        /*
         * Tell the remote router abput this connection
         */

        /*
         * Get name of remote router
         */

        try {
            routerResponse = interactor.getName();
        } catch (IOException ioexc) {
            System.err.println(leadin() + "Cannot GET_NAME from " + host + " -> " + ioexc);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot GET_NAME from host: " + host);
            return;
        } catch (MCRPException mcrpe) {
            System.err.println(leadin() + "Cannot GET_NAME from " + host + " -> " + mcrpe);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot GET_NAME from host: " + host);
            return;
        }

        // now get router name
        String remoteRouterName = routerResponse;
        
        /*
         * Interact with remote router
         */
        try {
            boolean interactionOK = false;

            // attempt this 3 times
            int attempts;
            for (attempts=0; attempts < 3; attempts++) {
                // we sleep a bit between each try
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    System.err.println("CC: SLEEP");
                }
                // send INCOMING_CONNECTION command

                try {
                    interactor.incomingConnection(latestConnectionId, controller.getName(), weight, socket.getLocalPort());

                    // connection setup ok
                    interactionOK = true;
                    break;
                } catch (Exception e) {
                    System.err.println(leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + e + ". Attempt: " + attempts);
                }

            }

            if (interactionOK) {
                // everything ok
            } else {
                // connection setup failed
                respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot interact with host: " + host);
                return;
            }

        } catch (Exception exc) {
            System.err.println(leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + exc);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot interact with host: " + host);
            return;
        }

        /*
         * Save the netIF.
         */
        controller.addNetIF(netIF);

        /*
         * Close connection to management port of remote router.
         */
        try { 
            interactor.quit();
        } catch (Exception e) {
            System.err.println(leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + e);
            respond(MCRP.CREATE_CONNECTION.ERROR + " CREATE_CONNECTION Cannot quit with host: " + host);
            return;
        }

        // close connection to management connection of remote router

        System.out.println(leadin() + "closed = " + host);

        // now plug netIF into Router
        netIF.setRemoteRouterName(remoteRouterName);

        RouterPort port = controller.plugInNetIF(netIF);


        // everything is successful
        respond(MCRP.CREATE_CONNECTION.CODE + " CREATE_CONNECTION " + latestConnectionId + " port" + port.getPortNo());

    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String CC = "CC: ";

        if (controller == null) {
            return CC;
        } else {
            return controller.getName() + " " + CC;
        }

    }


}
