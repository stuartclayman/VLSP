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
 * An application for adapting an Egress node
 * to send to a new UDP port.
 * It sends a message to the Egress node listening
 * on a Management port on usrAddr:usrPort
 * and informs it to update the forwarding
 * UDP address  udp-addr:udp-port.
 * <p>
 * AdaptEgress usrAddr:usrPort udp-addr:udp-port [optionals]
 * Optional args:
 * -d start-up delay (in milliseconds)
 * -v verbose
 */
public class AdaptEgress implements Application {
    SocketAddress mgmtUsrAddr = null;
    String udpAddr = null;
    String udpPort = null;

    // options
    int startDelay = 0;
    int verbose = 0;          // verbose level: 1 = normal 2=extra verboseness

    boolean running = false;
    DatagramSocket socket = null;


    /**
     * Constructor for AdaptEgress
     */
    public AdaptEgress() {
    }

    /**
     * Initialisation for AdaptEgress
     *  usrAddr:usrPort udp-addr:udp-port [optionals]
     * Optional args:
     * -d start-up delay (in milliseconds)
     * -v verbose
     */
    @Override
    public ApplicationResponse init(String[] args) {
        if (args.length >= 2) {
            Scanner scanner;

            /* get usr-addr:usr-path first */

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

                Logger.getLogger("log").logln(USR.ERROR, "ARG: usrAddr = " + mgmtUsrAddr);
            }

            /* get udp-addr:udp-port next */

            next = args[1];

            addrParts = next.split(":");

            if (addrParts.length != 2) {
                return new ApplicationResponse(false, "Bad Addr Spec " + next);

            } else {
                udpAddr = addrParts[0];
                udpPort = addrParts[1];

                // try address and see if it is valid
                try {
                    java.net.InetAddress inetAddr = java.net.InetAddress.getByName(udpAddr);
                    int inetPort = 0;

                    scanner = new Scanner(udpPort);
                    if (scanner.hasNextInt()) {
                        inetPort = scanner.nextInt();
                        scanner.close();
                    } else {
                    	scanner.close();
                        return new ApplicationResponse(false, "Bad port " + addrParts[1]);
                    }

                    new java.net.InetSocketAddress(inetAddr, inetPort);

                } catch (Exception e) {
                    return new ApplicationResponse(false, "UnknownHost " + addrParts[0]);
                }


                Logger.getLogger("log").logln(USR.ERROR, "ARG: udpAddr = " + udpAddr + ":" + udpPort);

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
            return new ApplicationResponse(false, "Usage: AdaptEgress address address");
        }
    }

    /** Start application with argument  */
    @Override
    public ApplicationResponse start() {
        try {
            // set up socket
            socket = new DatagramSocket();

            if (verbose > 0) {
                Logger.getLogger("log").logln(USR.ERROR, "About to connect to Socket on "+ mgmtUsrAddr);
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

    /** Implement graceful shut down */
    @Override
    public ApplicationResponse stop() {
        running = false;

        if (socket != null) {
            socket.close();

            Logger.getLogger("log").logln(USR.ERROR, "AdaptEgress stop");
        }


        return new ApplicationResponse(true, "");
    }

    /** Run the Send application */
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

        // embed udpAddr into JSONObject
        JSONObject jsobj = new JSONObject();

        try {
            jsobj.put("address", udpAddr + ":" + udpPort);
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

            Logger.getLogger("log").logln(USR.ERROR, "AdaptEgress close socket");
        //}

        Logger.getLogger("log").logln(USR.ERROR, "AdaptEgress: end of run()");

    }

}
