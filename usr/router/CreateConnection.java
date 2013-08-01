package usr.router;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.net.*;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import usr.interactor.RouterInteractor;



/**
 * A CreateConnection object, creates a connection from one router
 * to another.
 */
public class CreateConnection {
    RouterController controller;
    Request request;
    Response response;

    /**
     * Create a new connection.
     * CREATE_CONNECTION ip_addr/port - create a new network connection
     * with weight of 1, and a constructed name
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * connection to a router on the address ip_addr/port with a
     * connection weight of connection_weight and a constructed name
     * CREATE_CONNECTION ip_addr/port connection_weight connection_name -
     * create a new network
     * connection to a router on the address ip_addr/port with a
     * connection weight of connection_weight and a name
     * of connection_name
     */
    public CreateConnection(RouterController controller, Request request, Response response) {
        this.controller = controller;
        this.request = request;
        this.response = response;

    }

    public boolean run() throws IOException, JSONException {
        PrintStream out = response.getPrintStream();

        // get full request string
        String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
        // strip off /command
        String value = path.substring(9);



        String[] parts = value.split(" ");

        if (parts.length !=4 && parts.length != 3 && parts.length != 2) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "INVALID createConnection command: " + request);

            // HERE
            respondError("CREATE_CONNECTION wrong no of args");
            return false;
        }

        // check ip addr spec
        String[] ipParts = parts[1].split(":");

        if (ipParts.length != 2) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "INVALID createConnection ip address: " + request);
            respondError("CREATE_CONNECTION invalid address " + parts[1]);
            return false;
        }

        // process host and port
        String host = ipParts[0];
        Scanner sc = new Scanner(ipParts[1]);
        int portNumber;

        try {
            portNumber = sc.nextInt();
        } catch (Exception e) {
            respondError("CREATE_CONNECTION invalid port " + ipParts[1]);
            return false;
        }

        int weight;

        // if we have a 3rd arg
        if (parts.length == 3) {
            // get weight/distance factor
            sc = new Scanner(parts[2]);

            try {
                weight = sc.nextInt();
            } catch (Exception e) {
                respondError("CREATE_CONNECTION invalid weight " + parts[2]);
                return false;
            }
        } else {
            weight = 1;
        }

        String connectionName = null;

        // if we have a 4rd arg
        if (parts.length == 4) {
            connectionName = parts[3];
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
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown host: " + host);
            respondError("CREATE_CONNECTION Unknown host: " + host);
            return false;
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Cannot connect to " + host + " on port " + portNumber + " -> " + ioexc);
            respondError("CREATE_CONNECTION Cannot interact with host: " + host + " on port " + portNumber);
            return false;
        }

        // at this point we have a connection to
        // the managementSocket of the remote router

        try {
            routerResponse = interactor.getConnectionPort();
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot GET_CONNECTION_PORT from " + host + " -> " + ioexc);
            respondError("CREATE_CONNECTION Cannot GET_CONNECTION_PORT from host: " + host);
            return false;
        } catch (JSONException jsone) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot GET_CONNECTION_PORT from " + host + " -> " + jsone);
            respondError("CREATE_CONNECTION Cannot GET_CONNECTION_PORT from host: " + host);
            return false;
        }

        // Ok now we need to find the port the remote router
        // listens to for connections
        Scanner scanner = new Scanner(routerResponse);

        // now get connection port
        int connectionPort = scanner.nextInt();

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "createConnection: connectionPort at " + host + " is " +
                                      connectionPort);


        /*
         * Now connect to connections port of remote router.
         */

        // make a socket connection for the router - to - router path
        Socket socket = null;
        NetIF netIF = null;
        InetSocketAddress refAddr;
        String latestConnectionName = null;

        if (connectionName == null || connectionName.equals("")) {
            latestConnectionName = controller.getName() + ".Connection-" + controller.getConnectionCount();
        } else {
            latestConnectionName = connectionName;
        }

        try {
            // make a connection to a remote router
            TCPEndPointSrc src = new TCPEndPointSrc(host, connectionPort);
            netIF = new TCPNetIF(src, controller.getListener());

            // set its name
            netIF.setName(latestConnectionName);
            // set its weight
            netIF.setWeight(weight);

            // set its Address
            netIF.setAddress(controller.getAddress());

            netIF.connect();

            // get socket so we can determine the Inet Address
            socket = src.getSocket();

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "connection socket: " + socket);



            refAddr = new InetSocketAddress(socket.getInetAddress(), socket.getLocalPort());;

            // set its ID
            netIF.setID(refAddr.hashCode());

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "netif = " + netIF);


        } catch (UnknownHostException uhe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unknown host: " + host);
            respondError("CREATE_CONNECTION Unknown host: " + host);
            return false;
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin() + "Cannot connect to " + host + " on port " + connectionPort + " -> " + ioexc);
            respondError("CREATE_CONNECTION Cannot interact with host: " + host + " on port " + connectionPort);
            return false;
        }

        /*
         * Tell the remote router abput this connection
         */

        /*
         * Get name of remote router
         */

        String remoteRouterName;
        Address remoteRouterAddress;

        try {
            // now get router name
            remoteRouterName = interactor.getName();
            remoteRouterAddress = interactor.getAddress();
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot GET_NAME from " + host + " -> " + ioexc);
            respondError("CREATE_CONNECTION Cannot GET_NAME from host: " + host);
            return false;
        } catch (JSONException jsone) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot GET_NAME from " + host + " -> " + jsone);
            respondError("CREATE_CONNECTION Cannot GET_NAME from host: " + host);
            return false;
        }

        // set the remoteRouterName
        netIF.setRemoteRouterName(remoteRouterName);
        netIF.setRemoteRouterAddress(remoteRouterAddress);

        /*
         * Save the netIF temporarily
         */
        controller.registerTemporaryNetIF(netIF);


        /*
         * Interact with remote router
         */
        try {
            boolean interactionOK = false;

            // attempt this 3 times
            int attempts;

            for (attempts = 0; attempts < 3; attempts++) {
                // we sleep a bit between each try
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Logger.getLogger("log").logln(USR.ERROR, "CC: SLEEP");
                }

                // send INCOMING_CONNECTION command

                try {
                    interactor.incomingConnection(latestConnectionName, controller.getName(),
                                                  controller.getAddress(), weight, socket.getLocalPort());

                    // connection setup ok
                    interactionOK = true;
                    break;
                } catch (Exception e) {
                    Logger.getLogger("log").logln(USR.ERROR,
                                                  leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + e + ". Attempt: " +
                                                  attempts);
                }

            }

            if (interactionOK) {
                // everything ok
            } else {
                // connection setup failed
                respondError("CREATE_CONNECTION Cannot interact with host: " + host);
                return false;
            }

        } catch (Exception exc) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + exc);
            respondError("CREATE_CONNECTION Cannot interact with host: " + host);
            return false;
        }

        /*
         * Close connection to management port of remote router.
           try {
            // close connection to management connection of remote router
            interactor.quit();


            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "closed = " + host);

           } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + e);
            respondError("CREATE_CONNECTION Cannot quit with host: " + host);
            return false;
           }
         */

        // now plug the temporary netIF into Router
        RouterPort port = controller.plugTemporaryNetIFIntoPort(netIF);

        controller.newConnection();

        //respond(MCRP.CREATE_CONNECTION.CODE + " " es+ latestConnectionName); // + " port" + port.getPortNo());

        JSONObject jsobj = new JSONObject();

        jsobj.put("weight", netIF.getWeight());
        jsobj.put("name", netIF.getName());
        jsobj.put("address", netIF.getAddress().asTransmitForm());
        jsobj.put("remoteName",  netIF.getRemoteRouterName());
        jsobj.put("remoteAddress ", netIF.getRemoteRouterAddress().asTransmitForm());
        jsobj.put("port", port.getPortNo());

        out.println(jsobj.toString());
        response.close();

        return true;

    }

    /**
     * An error response
     */
    private void respondError(String msg) throws IOException, JSONException {
        JSONObject jsobj = new JSONObject();
        jsobj.put("error", msg);

        PrintStream out = response.getPrintStream();
        out.println(jsobj.toString());
        response.close();
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
