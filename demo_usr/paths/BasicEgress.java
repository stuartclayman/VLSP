package demo_usr.paths;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.DatagramSocket;


/**
 * An application which is an BasicEgress point for UDP data.
 * It listens on a USR port and forwards packets to
 * a UDP address:port combination.
 * <p>
 * BasicEgress usr-port udp-addr:udp-port [optinals]
 * Optional args:
 * -d start-up delay (in milliseconds)
 * -v verbose
 */
public class BasicEgress implements Application {
    int usrPort = 0;
    InetSocketAddress udpAddr = null;

    DatagramSocket inSocket = null;
    java.net.DatagramSocket outSocket = null;

    // options
    int startDelay = 0;
    int verbose = 0;          // verbose level: 1 = normal 2=extra verboseness


    // is the thread running
    boolean running = false;
    CountDownLatch latch = null;

    // USR Reader
    USRReader usrReader;
    Future<?>  usrReaderFuture;

    // UDP Forwarder
    UDPForwarder udpForwarder;
    Future<?>  udpForwarderFuture;

    public BasicEgress() {
        latch = new CountDownLatch(1);
    }

    /**
     * Initialisation for BasicEgress
     * BasicEgress usr-port udp-addr:udp-port [optinals]
     *
     */
    @Override
	public ApplicationResponse init(String[] args) {
        Scanner scanner;

        if (args.length >= 2) {
            // get path number
            String usrString = args[0];

            scanner = new Scanner(usrString);
            if (scanner.hasNextInt()) {
                usrPort = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                return new ApplicationResponse(false, "Bad USR port " + args[0]);
            }
            scanner.close();
            // get addr:path next

            String next = args[1];

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

                // Construct a SocketAddress
                udpAddr = new InetSocketAddress(addr, port);

            }
        } else {
            return new ApplicationResponse(false, "BasicEgress udpPort usrAddr:usrPort [optinals]");
        }

        // if there are no more args return
        if (args.length == 2) {
            return new ApplicationResponse(true, "");
        } else {
                // try and process extra args
            for (int extra=2; extra < args.length; extra++) {
                String thisArg = args[extra];

                // check if its a flag
                if (thisArg.charAt(0) == '-') {
                    // get option
                    char option = thisArg.charAt(1);

                    switch (option) {

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

    }



    /**
     * Start application
     */
    @Override
	public ApplicationResponse start() {
        try {
            LinkedBlockingDeque<usr.net.Datagram> queue = new LinkedBlockingDeque<usr.net.Datagram>();

            // allocate reader and forwarder
            usrReader = new USRReader(usrPort, queue, verbose);
            udpForwarder = new UDPForwarder(udpAddr, queue, verbose);

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

        usrReaderFuture.cancel(true);
        udpForwarderFuture.cancel(true);

        Logger.getLogger("log").logln(USR.STDOUT, "BasicEgress stop");

        // reduce latch count by 1
        latch.countDown();

        return new ApplicationResponse(true, "");
    }


    /**
     * Run the BasicEgress application
     *
     */
    @Override
	public void run()  {
        // Start Delay
        if (startDelay > 0) {
            try {
                Thread.sleep(startDelay);
            } catch (Exception e) {
            }
        }

        /*
         * Start the supporting threads that actually reads from the UDP socket
         * and forwards to the USR socket
         */
        ExecutorService executer = Executors.newFixedThreadPool(2);


        try {
            usrReaderFuture = executer.submit((Callable <?>)usrReader);

            udpForwarderFuture = executer.submit((Callable <?>)udpForwarder);

        } catch (Exception e) {
            Logger.getLogger("log").log(USR.ERROR, e.getMessage());
            e.printStackTrace();
        }

        // wait for the latch to drop before continuing
        try {
            latch.await();
        } catch (InterruptedException ie) {
        }



        usrReader.await();
        udpForwarder.await();


        Logger.getLogger("log").logln(USR.ERROR, "BasicEgress: end of run()");
    }

}
