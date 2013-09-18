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

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;


/**
 * An application which is an Egress point for UDP data.
 * It listens on a USR port and forwards packets to
 * a UDP address:port combination.
 * <p>
 * Egress usr-port udp-addr:udp-port [optinals]
 * Optional args:
 * -d start-up delay (in milliseconds)
 * -v verbose
 */
public class Egress implements Application, Reconfigure {
    int usrPort = 0;
    InetSocketAddress udpAddr = null;

    // options
    int startDelay = 0;
    int verbose = 0;          // verbose level: 1 = normal 2=extra verboseness


    ExecutorService executer;

    // is the thread running
    boolean running = false;
    CountDownLatch latch = null;

    // Main flow of Datagrams

    LinkedBlockingDeque<usr.net.Datagram> queue;

    // USR Reader
    USRReader usrReader;
    Future <?> usrReaderFuture;

    // UDP Forwarder
    UDPForwarder udpForwarder;
    Future <?> udpForwarderFuture;


    // Management interface

    LinkedBlockingDeque<usr.net.Datagram> mgmtQueue;

    // ManagementPort
    ManagementPort mPort;
    Future <?> mPortFuture;

    // ManagementListener
    ManagementListener mListener;
    Future<?> mListenerFuture;

    public Egress() {
        latch = new CountDownLatch(1);
    }

    /**
     * Initialisation for Egress
     * Egress usr-port udp-addr:udp-port [optinals]
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
            return new ApplicationResponse(false, "Egress udpPort usrAddr:usrPort [optinals]");
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
        // Main flow of Datagrams

        queue = new LinkedBlockingDeque<usr.net.Datagram>();

        try {
            // allocate reader
            usrReader = new USRReader(usrPort, queue, verbose);
        } catch (Exception e) {
            return startError("Cannot open reader socket " + usrPort + ": " + e.getMessage());
        }

        try {
            // allocate forwarder
            udpForwarder = new UDPForwarder(udpAddr, queue, verbose);
        } catch (Exception e) {
            return startError("Cannot open forwarder socket " + udpAddr + ": " + e.getMessage());
        }


        // Management interface

        mgmtQueue = new LinkedBlockingDeque<usr.net.Datagram>();
        int mgmtPort = (usrPort + 20000) % 32768;

        try {
            // allocate ManagementPort
            mPort = new ManagementPort(mgmtPort, mgmtQueue, verbose);
        } catch (Exception e) {
            return startError("Cannot open reader socket " + mgmtPort + ": " + e.getMessage());
        }


        try {
            // allocate ManagementListener
            mListener = new ManagementListener(new ReconfigureHandler(this), mgmtQueue, verbose);
        } catch (Exception e) {
            return startError("ManagementListener error " + ": " + e.getMessage());
        }



        running = true;



        return new ApplicationResponse(true, "");
    }

    /**
     * error
     */
    private ApplicationResponse startError(String msg) {
        Logger.getLogger("log").logln(USR.ERROR, msg);
        return new ApplicationResponse(false,  msg);
    }

    /**
     * Stop the application
     */
    @Override
	public ApplicationResponse stop() {
        running = false;

        usrReaderFuture.cancel(true);
        udpForwarderFuture.cancel(true);

        mPortFuture.cancel(true);
        mListenerFuture.cancel(true);


        Logger.getLogger("log").logln(USR.STDOUT, "Egress stop");

        // reduce latch count by 1
        latch.countDown();

        return new ApplicationResponse(true, "");
    }


    /**
     * Run the Egress application
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
        executer = Executors.newFixedThreadPool(5);


        try {
            usrReaderFuture = executer.submit((Callable <?>)usrReader);

            udpForwarderFuture = executer.submit((Callable <?>)udpForwarder);

            mPortFuture = executer.submit((Callable <?>)mPort);

            mListenerFuture = executer.submit((Callable <?>)mListener);

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

        mPort.await();
        mListener.await();

        Logger.getLogger("log").logln(USR.ERROR, "Egress: end of run()");
    }


    /**
     * Process a reconfiguration
     */
    @Override
	public Object process(JSONObject jsobj) {

        /* get new UDP address */

        InetSocketAddress newUdpAddr;

        try {
            // get addr:path next

            String next = jsobj.getString("address");

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
                Scanner scanner = new Scanner(addrParts[1]);
                if (scanner.hasNextInt()) {
                    port = scanner.nextInt();
                    scanner.close();
                } else {
                	scanner.close();
                    return new ApplicationResponse(false, "Bad port " + addrParts[1]);
                }
                scanner.close();
                // Construct a SocketAddress
                newUdpAddr = new InetSocketAddress(addr, port);

            }
        } catch (JSONException jse) {
            return new ApplicationResponse(false, "JSONException " + jse.getMessage());
        }

        /* now stop old Forwarder. */

        // send it a cancel
        udpForwarderFuture.cancel(true);

        // and wait for it
        udpForwarder.await();

        /* create new Forwarder */

        // allocate forwarder on new address
        try {
            udpForwarder = new UDPForwarder(newUdpAddr, queue, verbose);
        } catch (Exception e) {
            return new ApplicationResponse(false, "Cannot open forwarder socket " + newUdpAddr + ": " + e.getMessage());
        }


        try {
            udpForwarderFuture = executer.submit((Callable <?>)udpForwarder);
        } catch (Exception e) {
            return new ApplicationResponse(false, "Cannot submit task: " + e.getMessage());
        }


        Logger.getLogger("log").logln(USR.ERROR, "queue size = " + queue.size());

        return new ApplicationResponse(true, "started");
    }

}
