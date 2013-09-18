package demo_usr.paths;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Scanner;

import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;


/**
 * An application which is a netcat source for UDP data.
 * It listens on stdin and forwards packets to
 * a UDP address:port combination.
 * Optional args:
 * -i inter packet delay (in milliseconds)
 * -d start-up delay (in milliseconds)
 * -s size of UDP send buffer (in bytes) (default 1024)
 * -v verbose
 */
public class NetcatIn implements Application {
    InetSocketAddress udpAddr = null;

    int count = 0;
    int volume = 0;
    int interPacketDelay = 0;
    int startDelay = 0;
    int sendSize = 1024;
    boolean verbose = false;

    boolean running = false;

    java.net.DatagramSocket outSocket = null;

    public NetcatIn() {
    }

    /**
     * Initialisation for NetcatIn
     * NetcatIn udp-addr:udp-port
     *
     */
    @Override
	public ApplicationResponse init(String[] args) {
        Scanner scanner;

        if (args.length >= 1) {
            // get addr:path next

            String next = args[0];

            String[] addrParts = next.split(":");

            if (addrParts.length != 2) {
                return new ApplicationResponse(false, "Bad Addr Spec " + next);

            } else {
                // try and process addr and port
                InetAddress addr;
                int port;

                // try address
                try {
                    addr = InetAddress.getByName(addrParts[0]);

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
                udpAddr = new InetSocketAddress(addr, port);

            }
        } else {
            return new ApplicationResponse(false, "NetcatIn udpPort usrAddr:usrPort [optinals]");
        }

        // if there are no more args return
        if (args.length == 2) {
            return new ApplicationResponse(true, "");
        } else {
                // try and process extra args
            for (int extra=1; extra < args.length; extra++) {
                String thisArg = args[extra];

                // check if its a flag
                if (thisArg.charAt(0) == '-') {
                    // get option
                    char option = thisArg.charAt(1);

                    switch (option) {
                    case 'i': {
                        // get next arg
                        String argValue = args[++extra];

                        try {
                            interPacketDelay = Integer.parseInt(argValue);

                            if (interPacketDelay < 0) {
                                Logger.getLogger("log").logln(USR.ERROR, "interPacketDelay: " + (-interPacketDelay));
                            }


                        } catch (Exception e) {
                            return new ApplicationResponse(false, "Bad interPacketDelay " + argValue);
                        }
                        break;
                    }

                    case 'd': {
                        // get next arg
                        String argValue = args[++extra];

                        try {
                            startDelay = Integer.parseInt(argValue);
                        } catch (Exception e) {
                            return new ApplicationResponse(false, "Bad startDelay " + argValue);
                        }
                        break;
                    }

                    case 's': {
                        // get next arg
                        String argValue = args[++extra];

                        try {
                            sendSize = Integer.parseInt(argValue);

                            if (sendSize > 2048) {
                                sendSize = 2048;
                                Logger.getLogger("log").logln(USR.ERROR, "sendSize too big. set to: 2048");
                            }

                        } catch (Exception e) {
                            return new ApplicationResponse(false, "Bad sendSize " + argValue);
                        }
                        break;
                    }

                    case 'v': {
                        verbose = true;
                        break;
                    }


                    default:
                        return new ApplicationResponse(false, "Bad option " + option);
                    }
                }
            }

            return new ApplicationResponse(true, "");

        }

    }



    /**
     * Start application
     */
    @Override
	public ApplicationResponse start() {
        try {
            // set up outbound socket
            // don't bind() or connect() so we can set src and dst addr and port
            outSocket = new java.net.DatagramSocket();

            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            return new ApplicationResponse(false,  "Cannot open socket " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }


    /**
     * Stop the application
     */
    @Override
	public ApplicationResponse stop() {
        running = false;

        if (outSocket != null) {
            outSocket.close();
        }


        //Logger.getLogger("log").logln(USR.STDOUT, "NetcatIn stop");

        return new ApplicationResponse(true, "");
    }


    /**
     * Run the NetcatIn application
     *
     */
    @Override
	public void run()  {
        java.net.DatagramPacket outDatagram = null;

        byte[] udpBuffer = new byte[8192];
        outDatagram = new java.net.DatagramPacket(udpBuffer, 8192);

        BufferedInputStream bis = new BufferedInputStream(System.in);
        byte [] data = new byte[sendSize];

        // Start Delay
        if (startDelay > 0) {
            try {
                Thread.sleep(startDelay);
            } catch (Exception e) {
            }
        }

        long t0 = System.currentTimeMillis();

        try {
            while (running) {

                // read data from stdin
                int read = bis.read(data, 0, sendSize);


                if (read == -1) {
                    running = false;
                    break;
                }

                count++;
                volume += read;

                if (verbose) {
                    if (count % 100 == 0) {
                        Logger.getLogger("log").logln(USR.STDOUT, "NetcatIn count: " + count + " read: " + read + " volume: " + volume);
                    }
                }



                // now create a UDP Datagram
                // no need already created, but copy in data
                outDatagram.setData(data);
                outDatagram.setLength(read);

                // now work out where to send it

                // get dst address and port
                InetAddress dstAddr = udpAddr.getAddress();
                int dstPort = udpAddr.getPort();

                // set the dst address and port
                // the src addr and port will be set in send()
                outDatagram.setAddress(dstAddr);
                outDatagram.setPort(dstPort);


                try {
                    outSocket.send(outDatagram);


                    // Inter Packet Delay
                    if (interPacketDelay > 0) {
                        Thread.sleep(interPacketDelay);
                    } else if (interPacketDelay < 0) {
                        // -ve means 0 milliseconds and N nanoseconds
                        Thread.sleep(0, -interPacketDelay);
                    } else {
                        // interPacketDelay == 0
                        // no sleep
                    }


                } catch (Exception e) {
                    if (outSocket.isClosed()) {
                        break;
                    } else {
                        Logger.getLogger("log").logln(USR.STDOUT, "Cant send: " + outDatagram);
                    }
                }

            }

        } catch (SocketException se) {
            Logger.getLogger("log").log(USR.ERROR, se.getMessage());
        } catch (IOException ioe) {
            Logger.getLogger("log").log(USR.ERROR, ioe.getMessage());
        }

        long t1 = System.currentTimeMillis();

        long totalT = t1 - t0;
        Logger.getLogger("log").logln(USR.ERROR, "Run time = " + (totalT/1000) + ":" + (totalT%1000));


    }


    public static void main(String[] args) {
        // allocate a new logger
        Logger logger = Logger.getLogger("log");
        logger.addOutput(System.err, new BitMask(USR.ERROR));
        logger.addOutput(System.out, new BitMask(USR.STDOUT));


        NetcatIn nc = new NetcatIn();
        nc.init(args);
        nc.start();
        nc.run();
        nc.stop();
    }
}
