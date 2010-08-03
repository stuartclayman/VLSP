package usr.router;

import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;


/**
 * A CreateConnection object, creates a connection from one router
 * to another.
 */
public class CreateConnection implements Runnable {
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
    }

    public void run() {
        // process the request
        String value = request.value;
        SocketChannel channel = request.channel;

        // check command
        String[] parts = value.split(" ");
        if (parts.length != 3) {
            System.err.println("CC: INVALID createConnection command: " + request);
            controller.respond(channel, "401 CREATE_CONNECTION wrong no of args");
            return;
        }

        // check ip addr spec
        String[] ipParts = parts[1].split("/");
        if (ipParts.length != 2) {
            System.err.println("CC: INVALID createConnection ip address: " + request);
            controller.respond(channel, "401 CREATE_CONNECTION invalid address " + parts[1]);
            return;
        }

        // process host and port
        String host = ipParts[0];
        Scanner sc = new Scanner(ipParts[1]);
        int portNumber;

        try {
            portNumber = sc.nextInt();
        } catch (Exception e) {
            controller.respond(channel, "401 CREATE_CONNECTION invalid port " + ipParts[1]);
            return;
        }

        // get weight/distance factor
        sc = new Scanner(parts[2]);
        int weight;

        try {
            weight = sc.nextInt();
        }  catch (Exception e) {
            controller.respond(channel, "401 CREATE_CONNECTION invalid weight " + parts[2]);
            return;
        }

        // if we get here all the args seem OK

        /*
         * Connect to management port of remote router.
         */

        // initialise Socket to management connection of remote router
        Socket managementSocket = null;
        BufferedReader input = null;
        PrintWriter output = null;
        String routerResponse = null;

        try {
	    // make a socket connection to a remote router
            managementSocket = new Socket(InetAddress.getByName(host), portNumber);

            System.out.println("CC: socket to host " + host + " = " + managementSocket);

            // initialise I/O and communication sender -> receiver
            input = new BufferedReader(new InputStreamReader(managementSocket.getInputStream()));
            output = new PrintWriter(managementSocket.getOutputStream(), true);

        } catch (UnknownHostException uhe) {
            System.err.println("CC: Unknown host: " + host);
            controller.respond(channel, "401 CREATE_CONNECTION Unknown host: " + host);
            return;

        } catch (IOException ioexc) {
            System.err.println("CC: Cannot interact with " + host + " on port " + portNumber + " -> " + ioexc);
            controller.respond(channel, "401 CREATE_CONNECTION Cannot interact with host: " + host + " on port " + portNumber);
            return;
        }

        // at this point we have a connection to 
        // the managementSocket of the remote router

        /*
         * Interact with remote router
         */
        try {
            output.println("GET_CONNECTION_PORT");

            routerResponse = input.readLine(); 

        } catch (IOException ioexc) {
            System.err.println("CC: Cannot GET_CONNECTION_PORT from " + host + " -> " + ioexc);
            controller.respond(channel, "401 CREATE_CONNECTION Cannot GET_CONNECTION_PORT from host: " + host);
            return;
        }

        /*
         * Process the response
         */

        // TODO: check for null or ""
        if (routerResponse == null || routerResponse.equals("")) {
            controller.respond(channel, "401 CREATE_CONNECTION response for GET_CONNECTION_PORT from host: " + host + " is empty");
            return;
        }

        // Ok now we need to find the port the remote router
        // listens to for connections
        Scanner scanner = new Scanner(routerResponse);
        int code = scanner.nextInt();

        if (code != 203) {
            System.err.println("CC: Remote router at " + host + " did not return GET_CONNECTION_PORT correctly");
            controller.respond(channel, "401 CREATE_CONNECTION response for GET_CONNECTION_PORT from host: " + host + " is incorrect: " + routerResponse);
            return;
        }

        // now get connection port
        int connectionPort = scanner.nextInt();

        System.err.println("CC: createConnection: connectionPort at " + host + " is " + connectionPort);


        /*
         * Now connect to connections port of remote router.
         */

        // make a socket connection for the router - to - router path
        Socket connection = null;
        NetIF netIF = null;
        InetSocketAddress refAddr;
        String latestConnectionId = "/Router-" + controller.getName() + "/Connection-" + controller.getConnectionCount();

        try {
	    // make a socket connection to a remote router
            // connection = new Socket();

            SocketChannel connectionChannel = SocketChannel.open();
            //connectionChannel.configureBlocking(true);
            connection = connectionChannel.socket();

            InetSocketAddress routerConnections = new InetSocketAddress(InetAddress.getByName(host), connectionPort);


            // now connect
            connection.connect(routerConnections);

            System.err.println("CC: connection socket: " + connection);

            refAddr = new InetSocketAddress(connection.getInetAddress(), connection.getLocalPort());;
            //System.err.println("CreateConnection => " + refAddr + " # " + refAddr.hashCode());

            // wrap SocketChannel as a NetIF

            netIF = new TCPNetIF(connection);

            System.out.println("CC: netif = " + netIF);


        } catch (UnknownHostException uhe) {
            System.err.println("CC: Unknown host: " + host);
            controller.respond(channel, "401 CREATE_CONNECTION Unknown host: " + host);
            return;
        } catch (IOException ioexc) {
            System.err.println("CC: Cannot connect to " + host + " on port " + connectionPort + " -> " + ioexc);
            controller.respond(channel, "401 CREATE_CONNECTION Cannot interact with host: " + host + " on port " + connectionPort);
            return;
        }

        /*
         * Tell the remote router abput this connection
         */

        /*
         * Get name of remote router
         */
        try {
            output.println("GET_NAME");

            routerResponse = input.readLine(); 

        } catch (IOException ioexc) {
            System.err.println("CC: Cannot GET_NAME from " + host + " -> " + ioexc);
            controller.respond(channel, "401 CREATE_CONNECTION Cannot GET_NAME from host: " + host);
            return;
        }

        /*
         * Process the response
         */

        // TODO: check for null or ""
        if (routerResponse == null || routerResponse.equals("")) {
            controller.respond(channel, "401 CREATE_CONNECTION response for GET_NAME from host: " + host + " is empty");
            return;
        }

        // Ok now we need to find the name of the remote router
        parts = routerResponse.split(" ");
        

        if (!parts[0].equals("201")) {
            System.err.println("CC: Remote router at " + host + " did not return GET_NAME correctly");
            controller.respond(channel, "401 CREATE_CONNECTION response for GET_NAME from host: " + host + " is incorrect: " + routerResponse);
            return;
        }


        // now get router name
        String remoteRouterName = parts[1];
        


        /*
         * Interact with remote router
         */
        try {
            String icCode = "";
            String other = "";

            // attempt this 3 times
            for (int attempts=0; attempts < 3; attempts++) {
                // send INCOMING_CONNECTION command
                output.println("INCOMING_CONNECTION " + latestConnectionId + " " + controller.getName() + " " + weight + " " + connection.getLocalPort());

                routerResponse = input.readLine(); 

                String[] icParts = routerResponse.split(" ");
                icCode = icParts[0];
                other = icParts[1];

                if (icCode.equals("204")) {
                    // connection setup ok
                    break;
                }

                // if we get here, then the INCOMING_CONNECTION
                // did not succeed
                // so we sleep a bit
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }

            if (icCode.equals("204")) {
                // everything ok
            } else {
                // connection setup failed
                controller.respond(channel, routerResponse);
                return;
            }

        } catch (IOException ioexc) {
            System.err.println("CC: INCOMING_CONNECTION with host error " + host + " -> " + ioexc);
            controller.respond(channel, "401 CREATE_CONNECTION Cannot interact with host: " + host);
            return;
        }

        /*
         * Save the netIF.
         */
        controller.addNetIF(netIF);

        /*
         * Close connection to management port of remote router.
         */

        // close connection to management connection of remote router
        try {
            // close connection to remote router
            managementSocket.close();

            System.out.println("CC: closed = " + host);

        } catch (IOException ioexc) {
            System.err.println("CC: Cannot close connection to " + host);
            controller.respond(channel, "401 CREATE_CONNECTION Cannot close connection with host: " + host);
            return;
        }

        // now plug netIF into Router
        netIF.setName(latestConnectionId);
        netIF.setWeight(weight);
        netIF.setID(refAddr.hashCode());
        netIF.setRemoteRouterName(remoteRouterName);

        RouterPort port = controller.plugInNetIF(netIF);


        // everything is successful
        controller.respond(channel, "299 CREATE_CONNECTION " + latestConnectionId + " port" + port.getPortNo());

    }
}
