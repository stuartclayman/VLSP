package usr.router;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.interactor.RouterInteractor;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.common.ANSI;


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
            sc.close();
        } catch (Exception e) {
            respondError("CREATE_CONNECTION invalid port " + ipParts[1]);
            sc.close();
            return false;
        }

        int weight;

        // if we have a 3rd arg
        if (parts.length == 3) {
            // get weight/distance factor
            sc = new Scanner(parts[2]);

            try {
                weight = sc.nextInt();
                sc.close();
            } catch (Exception e) {
                respondError("CREATE_CONNECTION invalid weight " + parts[2]);
                sc.close();
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
        sc.close();
        // Create a RouterInteractor to the remote Router
        RouterInteractor interactor = null;
        String routerResponse;


        try {
            if (host.equals("localhost")) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "createConnection: 'localhost' -> " + InetAddress.getByName(host)); //InetAddress.getLocalHost().getHostAddress());
                //interactor = new RouterInteractor(InetAddress.getByName(host), portNumber);
                interactor = new RouterInteractor(InetAddress.getLocalHost().getHostAddress(), portNumber);
            } else {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "createConnection: " + host + " -> " + InetAddress.getLocalHost().getHostAddress());
                interactor = new RouterInteractor(InetAddress.getByName(host), portNumber);
            }

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
        scanner.close();
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "createConnection: connectionPort at " + host + " is " + connectionPort);


        /*
         * Now connect to connections port of remote router.
         */

        // make a socket connection for the router - to - router path
        NetIF netIF = null;
        String latestConnectionName = null;
        int connectionHashCode;

        if (connectionName == null || connectionName.equals("")) {
            latestConnectionName = controller.getName() + ".Connection-" + controller.getConnectionCount();
        } else {
            latestConnectionName = connectionName;
        }

        try {
            // make a connection to a remote router
            netIF = controller.getRouterConnections().getNetIFSrc(host, connectionPort);

            // set its name
            netIF.setName(latestConnectionName);
            // set its weight
            netIF.setWeight(weight);

            // set its Address
            netIF.setAddress(controller.getAddress());

            // connect to other router
            netIF.connectPhase1();


            connectionHashCode = controller.getRouterConnections().getCreateConnectionHashCode(netIF, interactor.getInetAddress(), connectionPort);

            Logger.getLogger("log").logln(USR.STDOUT,  leadin() + "CreateConnection hashCode for address " + interactor.getInetAddress() + " => "  + " # " + connectionHashCode);
                

            // set its ID
            netIF.setID(connectionHashCode);

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

        int remotePort = -1;
        
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

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "incomingConnection: " + " host = " + netIF.getLocalAddress() + " port = " + netIF.getLocalPort() + " hashCode = " + connectionHashCode);
                try {
                    // send connectionHashCode and the details for the socket at this end
                    JSONObject result = interactor.incomingConnection(latestConnectionName, controller.getName(),
                                                  controller.getAddress(), weight, connectionHashCode,
                                                  netIF.getLocalAddress(), netIF.getLocalPort()  );

                    String errMSg = result.optString("error");

                    if ("".equals(errMSg)) {  // no error msg
                        // connection setup ok
                        interactionOK = true;

                        remotePort = result.optInt("routerPort");
                    }
                    
                    break;
                } catch (Exception e) {
                    //e.printStackTrace();
                    Logger.getLogger("log").logln(USR.ERROR,
                                                  ANSI.RED + leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + e + ". Attempt: " + attempts + ANSI.RESET_COLOUR);
                }

            }

            if (interactionOK) {
                // everything ok

                // connect to other router
                netIF.connectPhase2();

            } else {
                // connection setup failed
                respondError("CREATE_CONNECTION Cannot interact with host: " + host);
                return false;
            }

        } catch (Exception exc) {
            //exc.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR,  ANSI.RED_BG +leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + exc + ANSI.RESET_COLOUR);
            respondError("CREATE_CONNECTION Cannot interact with host: " + host);
            return false;
        }

        /*
         * Close connection to management port of remote router.
         */
           try {
            // close connection to management connection of remote router
            interactor.quit();


            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "closed = " + host);

           } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "INCOMING_CONNECTION with host error " + host + " -> " + e);
            respondError("CREATE_CONNECTION Cannot quit with host: " + host);
            return false;
           }

        // now plug the temporary netIF into Router
        RouterPort port = controller.plugTemporaryNetIFIntoPort(netIF);

        controller.newConnection();

        //respond(MCRP.CREATE_CONNECTION.CODE + " " es+ latestConnectionName); // + " port" + port.getPortNo());

        JSONObject jsobj = new JSONObject();

        jsobj.put("weight", netIF.getWeight());
        jsobj.put("port", port.getPortNo());

        jsobj.put("name", netIF.getName());
        jsobj.put("address", netIF.getAddress().asTransmitForm());
        jsobj.put("remotePort", remotePort);
        jsobj.put("remoteName",  netIF.getRemoteRouterName());
        jsobj.put("remoteAddress ", netIF.getRemoteRouterAddress().asTransmitForm());

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
