package demo_usr.paths;

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
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.SocketAddress;


/**
 * An application which is an Ingress point for UDP data.
 * It listens on a UDP port and forwards packets to
 * a USR address:port combination.
 * <p>
 * Ingress udp-port usr-addr:usr-port [optionals]
 * Optional args:
 * -d start-up delay (in milliseconds)
 * -b size of UDP recv buffer (in kilobytes) (default 8K)
 * -v verbose
 */
public class Ingress implements Application, Reconfigure {
    int udpPort = 0;
    SocketAddress usrAddr = null;

    // options
    int startDelay = 0;
    int verbose = 0;          // verbose level: 1 = normal 2=extra verboseness

    // The UDP recv buffer size in K
    int recvBufSize = 8;

    ExecutorService executer;

    // is the thread running
    boolean running = false;
    CountDownLatch latch = null;

    // Main flow of Datagrams

    LinkedBlockingDeque<usr.net.Datagram> queue;

    // UDP Reader
    UDPReader udpReader;
    Future<?> udpReaderFuture;

    // USR Forwarder
    USRForwarder usrForwarder;
    Future<?> usrForwarderFuture;

    // Management interface
    LinkedBlockingDeque<usr.net.Datagram> mgmtQueue;

    // ManagementPort
    ManagementPort mPort;
    Future<?> mPortFuture;

    // ManagementListener
    ManagementListener mListener;
    Future<?> mListenerFuture;

    /**
     * Forward data from a UDP port to a USR port.
     */
    public Ingress() {
        latch = new CountDownLatch(1);
    }

    /**
     * Initialisation for Ingress
     * Ingress udp-port usr-addr:usr-port [optionals]
     *
     */
    @Override
	public ApplicationResponse init(String[] args) {
        Scanner scanner;

        if (args.length >= 2) {
            // get path number
            String udpString = args[0];

            scanner = new Scanner(udpString);
            if (scanner.hasNextInt()) {
                udpPort = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                return new ApplicationResponse(false, "Bad UDP port " + args[0]);
            }
            scanner.close();
            // get addr:path next

            String next = args[1];

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

                // Construct a SocketAddress
                usrAddr = new SocketAddress(addr, port);

            }
        } else {
            return new ApplicationResponse(false, "Usage: Ingress udpPort usrAddr:usrPort [optionals]");
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

                    case 'b': {
                        // get next arg
                        String argValue = args[++extra];

                        try {
                            recvBufSize = Integer.parseInt(argValue);
                        } catch (Exception e) {
                            return new ApplicationResponse(false, "Bad sendSize " + argValue);
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
        queue = new LinkedBlockingDeque<usr.net.Datagram>();

        try {
            // allocate reader
            udpReader = new UDPReader(udpPort, recvBufSize, queue, verbose);

        } catch (Exception e) {
            return startError( "Cannot open reader socket " +  udpPort + ": " + e.getMessage());
        }


        try {
            // allocate forwarder
            usrForwarder = new USRForwarder(usrAddr, queue, verbose);

        } catch (Exception e) {
            return startError("Cannot open forwarder socket " + usrAddr.getPort() + ": " + e.getMessage());
        }


        // Management interface

        mgmtQueue = new LinkedBlockingDeque<usr.net.Datagram>();
        int mgmtPort = (udpPort + 20000) % 32768;

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

        udpReaderFuture.cancel(true);
        usrForwarderFuture.cancel(true);

        mPortFuture.cancel(true);
        mListenerFuture.cancel(true);


        Logger.getLogger("log").logln(USR.ERROR, "Ingress stop");

        // reduce latch count by 1
        latch.countDown();

        return new ApplicationResponse(true, "");
    }


    /**
     * Run the Ingress application
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
            udpReaderFuture = executer.submit((Callable <?>)udpReader);

            usrForwarderFuture = executer.submit((Callable <?>)usrForwarder);

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


        udpReader.await();
        usrForwarder.await();

        mPort.await();
        mListener.await();

        Logger.getLogger("log").logln(USR.ERROR, "Ingress: end of run()");


    }



    /**
     * Process a reconfiguration
     */
    @Override
	public Object process(JSONObject jsobj) {

        /* get new USR address */

        SocketAddress newUsrAddr;

        try {
            // get addr:path next

            String next = jsobj.getString("address");

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
                newUsrAddr = new SocketAddress(addr, port);

            }
        } catch (JSONException jse) {
            return new ApplicationResponse(false, "JSONException " + jse.getMessage());
        }

        /* now stop old Forwarder. */

        // send it a cancel
        usrForwarderFuture.cancel(true);

        // and wait for it
        usrForwarder.await();

        Logger.getLogger("log").logln(USR.ERROR, "queue size = " + queue.size());

        /* create new Forwarder */

        // allocate forwarder on new address
        try {
            usrForwarder = new USRForwarder(newUsrAddr, queue, verbose);
        } catch (Exception e) {
            return new ApplicationResponse(false, "Cannot open forwarder socket " + newUsrAddr + ": " + e.getMessage());
        }


        try {
            usrForwarderFuture = executer.submit((Callable <?>)usrForwarder);
        } catch (Exception e) {
            return new ApplicationResponse(false, "Cannot submit task: " + e.getMessage());
        }


        return new ApplicationResponse(true, "started");
    }

}
