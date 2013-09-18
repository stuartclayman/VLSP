package demo_usr.paths;

import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;
import usr.net.SocketAddress;

/**
 * An application for Sending some data
 * split over 2 or more potential destinations.
 * <p>
 * Split usrPort -path 1 addr1:port -path 2 addr2:port [-path i nexthost:port] -split 0.4 0.6 [optional ratio] [optionals]
 * Optional args:
 * -i inter packet delay (in milliseconds)
 * -d start-up delay (in milliseconds)
 * -v verbose
 */
public class Split implements Application {
    int usrPort = 0;

    DatagramSocket inSocket = null;
    DatagramSocket outSocket = null;


    // keep track of data
    // total no of Datagrams in
    int count = 0;
    // last time count
    int lastTimeCount = 0;
    // total volume of data in
    long volume = 0;
    // last time volume
    long lastTimeVolume = 0;


    int interPacketDelay = 0;
    int startDelay = 0;
    int sendSize = 0;

    boolean verbose = false;
    boolean verbose2 = false;   // extra verboseness

    boolean running = false;

    // Vector for splitting ratios
    Vector<Float> ratios;
    // PathForwardingTable
    Vector<SocketAddress> pathForwardingTable;
    // A Random number generator
    Random generator;

    /**
     * Constructor for Split
     */
    public Split() {
        ratios = new Vector<Float>();
        pathForwardingTable = new Vector<SocketAddress>();
        generator = new Random(System.currentTimeMillis());
    }

    /**
     * Initialisation for Split
     * Split usrPort -path 1 addr1:port -path 2 addr2:port [-path i nexthost:port] -split 0.4 0.6 [optional ratios] [optionals]
     *
     */
    @Override
	public ApplicationResponse init(String[] args) {
        int argc = args.length;
        int current = 0;
        int pathCount = 0;
        int lastPath = 0;
        Scanner scanner;

        if (args.length >= 10) {
            // get port number
            String usrString = args[0];
            current++;

            scanner = new Scanner(usrString);
            if (scanner.hasNextInt()) {
                usrPort = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                return new ApplicationResponse(false, "Bad USR port " + args[0]);
            }
            scanner.close();

            while (true) {
                // skip through N path specifiers

                //System.err.println("arg: " + args[current]);

                if (args[current].equals("-path")) {
                    // get path number
                    String pathString = args[current+1];
                    int pathNo = 0;

                    scanner = new Scanner(pathString);
                    if (scanner.hasNextInt()) {
                        pathNo = scanner.nextInt();
                        scanner.close();
                    } else {
                    	scanner.close();
                        return new ApplicationResponse(false, "Bad pathNo " + args[current+1]);
                    }

                    // check path no looks ok
                    if ((pathCount > 0 && pathNo <= lastPath) || (pathCount > 0 && pathNo > (lastPath + 1))) {
                        return new ApplicationResponse(false, "Bad sequence for pathNo " + pathNo);
                    }

                    lastPath = pathNo;


                    // get addr:path next

                    String pathNext = args[current+2];

                    String[] addrParts = pathNext.split(":");

                    if (addrParts.length != 2) {
                        return new ApplicationResponse(false, "Bad Addr Spec " + pathNext);

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
                        SocketAddress socketAddress = new SocketAddress(addr, port);

                        // and put in PathForwardingTable
                        pathForwardingTable.add(pathCount, socketAddress);

                        current += 3;
                        pathCount++;

                    }
                } else if (args[current].equals("-split")) {
                    // need to get pathCount ratio values
                    current++;

                    if (pathCount == 0) {
                        return new ApplicationResponse(false, "No paths specified");
                    } else {

                        // check if there are enough args
                        if (args.length - current < pathCount) {
                            return new ApplicationResponse(false, "Not enough ratios specified");
                        }

                        for (int r=0; r < pathCount; r++) {
                            Float ratio;

                            scanner = new Scanner(args[current]);
                            if (scanner.hasNextFloat()) {
                                ratio = scanner.nextFloat();

                                // add ratio to ratio table
                                ratios.add(ratio);
                                current++;
                                scanner.close();
                            } else {
                            	scanner.close();
                                return new ApplicationResponse(false, "Bad ratio " + args[current]);
                            }
                        }

                        // now check all the ratios add up to 1
                        float sum = 0.0f;
                        for (int r=0; r<ratios.size(); r++) {
                            sum += ratios.get(r);
                        }

                        if (sum != 1.0) {
                            return new ApplicationResponse(false, "Ratios do not add up to 1.0");
                        }

                        // finished the main arg loop
                        break;
                    }
                }

            }



            if (current == argc) {
                return new ApplicationResponse(true, "");
            } else {
                // try and process extra args
                for (int extra=current; extra < args.length; extra++) {
                    String thisArg = args[extra];

                    System.err.println("arg: " + thisArg);

                    // check if its a flag
                    if (thisArg.charAt(0) == '-') {
                        // get option
                        char option = thisArg.charAt(1);

                        switch (option) {
                        case 'i': {
                            // gwet next arg
                            String argValue = args[++extra];

                            try {
                                interPacketDelay = Integer.parseInt(argValue);
                            } catch (Exception e) {
                                return new ApplicationResponse(false, "Bad interPacketDelay " + argValue);
                            }
                            break;
                        }

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

                        case 's': {
                            // gwet next arg
                            String argValue = args[++extra];

                            try {
                                sendSize = Integer.parseInt(argValue);
                            } catch (Exception e) {
                                return new ApplicationResponse(false, "Bad sendSize " + argValue);
                            }
                            break;
                        }

                        case 'v': {
                            verbose = true;

                            if (thisArg.length() == 3 && thisArg.charAt(2) == 'v') {
                                verbose2 = true;
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
            return new ApplicationResponse(false, "Usage: Split usrPort -path 1 addr1:port -path 2 addr2:port [-path i nexthost:port] -split 0.4 0.6  [optionals]");
        }
    }

    /**
     * Start application
     */
    @Override
	public ApplicationResponse start() {
        try {
            // set up inbound socket
            inSocket = new DatagramSocket();

            inSocket.bind(usrPort);

            // set up outbound socket
            // don't bind() or connect() so we can set src and dst addr and port
            outSocket = new DatagramSocket();

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
        // interrupt queue.take()
        Thread.currentThread().interrupt();

        running = false;

        if (inSocket != null) {
            inSocket.close();
        }

        if (outSocket != null) {
            outSocket.close();
        }


        Logger.getLogger("log").logln(USR.STDOUT, "Split stop");

        return new ApplicationResponse(true, "");
    }




     /**
     * Run the Forward application
     *
     */
    @Override
	public void run()  {
        Datagram inDatagram = null;
        Datagram outDatagram = null;

        /*
         * Start the supporting thread that actually reads from the socket.
         */
        LinkedBlockingDeque<Datagram> queue = new LinkedBlockingDeque<Datagram>();
        ExecutorService executer = Executors.newFixedThreadPool(1);
        executer.submit((Callable <?>)
            new ReadThread(inSocket, queue));



        // Start Delay
        if (startDelay > 0) {
            try {
                Thread.sleep(startDelay);
            } catch (Exception e) {
            }
        }

        try {

            while (running) {

                do {
                    inDatagram = queue.take();
                    if (inDatagram == null) {
                        Logger.getLogger("log").logln(USR.ERROR, "Ingress IN DatagramPacket is NULL " + count + " volume: " + volume + " at " + System.currentTimeMillis());
                    }
                } while (inDatagram == null);

                count++;

                // get a copy of the payload
                byte[] data = inDatagram.getPayload();
                volume += inDatagram.getLength();

                /*
                if (verbose2) {
                    if (count % 100 == 0) {
                        Logger.getLogger("log").logln(USR.STDOUT, "Forward " + count + " recv: " + data.length +  " volume: " + volume);
                    }
                }
                */


                // now create a USR Datagram
                outDatagram = DatagramFactory.newDatagram(data);


                //System.err.println("Ingress OUT " + count + " recv: " +  outDataLength + " volume: " + volume + " at " + System.currentTimeMillis());

                // get dst address and port
                // this is set using a split - once per datagram
                // rather than being set outside of the loop


                // we get a random number and determine which element
                // of the pathForwardingTable to get
                int pathForwardingTableOffset = findNextForwardingPath();

                SocketAddress usrAddr = pathForwardingTable.get(pathForwardingTableOffset);

                Address dstAddr = usrAddr.getAddress();
                int dstPort = usrAddr.getPort();

                // set the dst address and port
                // the src addr and port will be set in send()
                // this will set based on a split
                outDatagram.setDstAddress(dstAddr);
                outDatagram.setDstPort(dstPort);



                try {
                    outSocket.send(outDatagram);

                    // Inter Packet Delay
                    if (interPacketDelay > 0) {
                        Thread.sleep(interPacketDelay);
                    }


                } catch (Exception e) {
                    if (outSocket.isClosed()) {
                        break;
                    } else {
                        Logger.getLogger("log").logln(USR.ERROR, "Cant send: " + outDatagram);
                    }
                }

            }

        } catch (Exception e) {
            Logger.getLogger("log").log(USR.ERROR, e.getMessage());
            e.printStackTrace();
        }


        //Logger.getLogger("log").logln(USR.STDOUT, "Forward: end of run()");


    }


    /**
     * Find the next forwarding path.
     * It takes the ratios vector and determines which path to choose next.
     * As the ratios add up to 1, we take a view on the ratios vector
     * which accumulates.
     * e.g [0.5 0.4 0.1]   ->     [0.5  0.9  1.0]
     */
    protected int findNextForwardingPath() {
        float next = generator.nextFloat();

        float sum = 0.0f;

        for (int r=0;  r < ratios.size(); r++) {
            sum +=  ratios.get(r);

            if (next < sum) {
                return r;
            }
        }


        throw new Error("Split findNextForwardingPath() should never get here");
    }

    /**
     * Print some info
     */
    void printInfo() {
        System.out.println("usrPort: " + usrPort);

        for (int p=0; p < pathForwardingTable.size(); p++) {
            System.out.println("path " + p + " -> " + pathForwardingTable.get(p) + " ratio: " +
                               ratios.get(p));
        }
    }

    /**
     * Read Datagrams from a DatagramSocket
     */
    class ReadThread implements Callable <Object> {
        DatagramSocket socket;
        LinkedBlockingDeque<Datagram> queue;
        boolean running = false;

        public ReadThread(DatagramSocket socket, LinkedBlockingDeque<Datagram> queue) {
            // the socket to read from
            this.socket = socket;
            // The queue to put new Datagrams on
            this.queue = queue;

            running = true;
        }


        @Override
		public Object call() {
            // allocate a Datagram
            Datagram inDatagram = null;



            //Thread.currentThread().setPriority( Thread.NORM_PRIORITY - 2);

            try {
                while (running) {
                    // read a USR Datagram
                    inDatagram = inSocket.receive();
                    queue.add(inDatagram);

                    //System.err.println("ReadThread IN  " + count + " recv: " +  inDatagram.getLength());
                    /*
                    if (count % 100 == 0) {

                        System.err.println("ReadThread " + count + " queue size: " + queue.size());
                    }
                    */
                }
            } catch (SocketException se) {
                Logger.getLogger("log").log(USR.ERROR, se.getMessage());


            } catch (Throwable t) {
                Logger.getLogger("log").log(USR.ERROR, t.getMessage());
                t.printStackTrace();
            }


            return null;

        }
    }

    public static void main(String[] args) {
        // allocate a new logger
        Logger logger = Logger.getLogger("log");
        logger.addOutput(System.err, new BitMask(USR.ERROR));
        logger.addOutput(System.out, new BitMask(USR.STDOUT));


        Split nc = new Split();
        ApplicationResponse initR = nc.init(args);

        if (!initR.isSuccess()) {
            System.out.println(initR.getMessage());
            return;
        }

        nc.printInfo();

        for (int x=0; x<128; x++) {
            System.out.print(nc.findNextForwardingPath() + "    ");
        }
        System.out.println();
    }


}
