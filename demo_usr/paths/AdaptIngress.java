package demo_usr.paths;

import java.util.Scanner;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;
import usr.net.SocketAddress;

/**
 * An application for adapting an Ingress node
 * to send to a new USR port.
 * It sends a message to the Ingress node listening
 * on a Management port on usrAddr:usrPort
 * and informs it to update the forwarding
 * USR address  usr-addr:usr-port.
 * <p>
 * AdaptIngress usrAddr:usrPort usr-addr:usr-port [optionals]
 * Optional args:
 * -d start-up delay (in milliseconds)
 * -v verbose
 */
public class AdaptIngress implements Application {
    SocketAddress mgmtUsrAddr = null;
    String usrAddr = null;
    String usrPort = null;

    // options
    int startDelay = 0;
    int verbose = 0;          // verbose level: 1 = normal 2=extra verboseness

    boolean running = false;
    DatagramSocket socket = null;


    /**
     * Constructor for AdaptIngress
     */
    public AdaptIngress() {
    }

    /**
     * Initialisation for AdaptIngress
     *  usrAddr:usrPort usr-addr:usr-port [optionals]
     * Optional args:
     * -d start-up delay (in milliseconds)
     * -v verbose
     */
    @Override
    public ApplicationResponse init(String[] args) {
        if (args.length >= 2) {
            Scanner scanner;

            /* get management usrAddr:usrPort first */

            String next = args[0];

            String[] addrParts = next.split(":");

            if (addrParts.length != 2) {
                return new ApplicationResponse(false, "Bad Addr Spec " + next);

            } else {
                // try and process addr and port
                Address addr;
                int port;

                // try address
                try {
                    addr = AddressFactory.newAddress(addrParts[0]);

                } catch (Exception e) {
                    return new ApplicationResponse(false, "UnknownHost " + addrParts[0]);
                }

                // try port
                scanner = new Scanner(addrParts[1]);
                if (scanner.hasNextInt()) {
                    port = scanner.nextInt();
                    scanner.close();
                } else {
                    scanner.close();
                    return new ApplicationResponse(false, "Bad port " + addrParts[1]);
                }
                scanner.close();
                // Construct a SocketAddress
                mgmtUsrAddr = new SocketAddress(addr, port);

                Logger.getLogger("log").logln(USR.ERROR, "ARG: mgmt usrAddr = " + usrAddr);
            }

            /* get usr-addr:usr-port next */

            next = args[1];

            addrParts = next.split(":");

            if (addrParts.length != 2) {
                return new ApplicationResponse(false, "Bad Addr Spec " + next);

            } else {
                usrAddr = addrParts[0];
                usrPort = addrParts[1];

                // try address and see if it is valid
                try {
                    Address testAddr = AddressFactory.newAddress(usrAddr);
                    int testPort = 0;

                    scanner = new Scanner(usrPort);
                    if (scanner.hasNextInt()) {
                        testPort = scanner.nextInt();
                        scanner.close();
                    } else {
                    	scanner.close();
                        return new ApplicationResponse(false, "Bad port " + addrParts[1]);
                    }

                    new SocketAddress(testAddr, testPort);

                } catch (Exception e) {
                    return new ApplicationResponse(false, "UnknownHost " + addrParts[0]);
                }


                Logger.getLogger("log").logln(USR.ERROR, "ARG: usrAddr = " + usrAddr + ":" + usrPort);

            }

            if (args.length == 2) {
                return new ApplicationResponse(true, "");
            } else {
                // try and process extra args
                for (int extra = 2; extra < args.length; extra++) {
                    String thisArg = args[extra];

                    // check if its a flag
                    if (thisArg.charAt(0) == '-') {
                        // get option
                        char option = thisArg.charAt(1);

                        switch (option) {

                        case 'd': {
                            // gwet next arg
                            String argValue = args[++extra];

                            try {
                                startDelay = Integer.parseInt(argValue);
                            } catch (Exception e) {
                                return new ApplicationResponse(false, "Bad startDelay " + argValue);
                            }

                            break;
                        }

                        case 'v': {
                            verbose = 1;
                            if (thisArg.length() == 3 && thisArg.charAt(2) == 'v') {
                                verbose = 2;
                            }
                            break;
                        }



                        default:
                            return new ApplicationResponse(false, "Bad option " + option);
                        }
                    }
                }

                return new ApplicationResponse(true, "");

            }

        } else {
            return new ApplicationResponse(false, "Usage: AdaptIngress address address");
        }
    }

    /**
     * Start application
     */
    @Override
    public ApplicationResponse start() {
        try {
            // set up socket
            socket = new DatagramSocket();

            if (verbose > 0) {
                Logger.getLogger("log").logln(USR.ERROR, "About to connect to Socket on "+ usrAddr);
            }

            socket.connect(mgmtUsrAddr);

            if (verbose > 0) {
                Logger.getLogger("log").logln(USR.ERROR, "Socket connect  to "+ socket.getRemoteAddress());
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            e.printStackTrace();
            return new ApplicationResponse(false, "Cannot open socket " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

    /**
     * Implement graceful shut down
     */
    @Override
    public ApplicationResponse stop() {
        running = false;

        if (socket != null) {
            socket.close();

            Logger.getLogger("log").logln(USR.ERROR, "AdaptIngress stop");
        }


        return new ApplicationResponse(true, "");
    }

    /**
     * Run the application
     */
    @Override
    public void run() {
        Datagram datagram = null;

        // Start Delay
        if (startDelay > 0) {
            try {
                Thread.sleep(startDelay);
            } catch (Exception e) {
            }
        }

        // embed usrAddr into JSONObject
        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("address", usrAddr + ":" + usrPort);
        } catch (JSONException jse) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONException " + jse);
            return;
        }

        String jsString = jsobj.toString();


        if (verbose > 0) {
            Logger.getLogger("log").logln(USR.ERROR, "JSONObject = " + jsString);
        }

        byte[] buffer = jsString.getBytes();

        datagram = DatagramFactory.newDatagram(buffer);

        // now send it

        try {
            socket.send(datagram);

        } catch (Exception e) {
            if (socket.isClosed()) {
                Logger.getLogger("log").logln(USR.ERROR, "Cant send: socket closed with " + jsString);
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "Cant send: " + e + " with " + jsString);
            }
        }

        // cannot be null, if we get here
        //if (socket != null) {
        socket.close();

        Logger.getLogger("log").logln(USR.ERROR, "AdaptIngress close socket");
        //}

        Logger.getLogger("log").logln(USR.ERROR, "AdaptIngress: end of run()");

    }

}
