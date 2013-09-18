package demo_usr.paths;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

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
 * An application which is an Ingress point for UDP data.
 * It listens on a UDP port and forwards packets to
 * a USR address:port combination.
 * <p>
 * IngressS udp-port usr-addr:usr-port [optionals]
 * Optional args:
 * -i inter packet delay (in milliseconds)
 * -d start-up delay (in milliseconds)
 * -b size of UDP recv buffer (in kilobytes) (default 8K)
 * -v verbose
 */
public class IngressS implements Application {
    // in and out network info
    int udpPort = 0;
    SocketAddress usrAddr = null;

    java.net.DatagramSocket inSocket = null;
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


    // Timer stuff
    Timer timer;
    TimerTask dataRateEvaluator;
    long startTime = 0;
    long lastTime = 0;

    // options
    int interPacketDelay = 0;
    int startDelay = 0;
    boolean verbose = false;

    // The UDP recv buffer size in K
    int recvBufSize = 8;

    // is the thread running
    boolean running = false;


    public IngressS() {
    }

    /**
     * Initialisation for IngressS
     * IngressS udp-port usr-addr:usr-port [optionals]
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
            return new ApplicationResponse(false, "Usage: IngressS udpPort usrAddr:usrPort [optinals]");
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
                    case 'i': {
                        // get next arg
                        String argValue = args[++extra];

                        try {
                            interPacketDelay = Integer.parseInt(argValue);
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
            // set up inbound UDP socket
            //inSocket = new java.net.DatagramSocket(udpPort);

            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            inSocket = channel.socket();
            inSocket.bind(new InetSocketAddress(udpPort));

            inSocket.setReceiveBufferSize(recvBufSize * 1024);


            if (verbose) {
                Logger.getLogger("log").logln(USR.STDOUT, "Ingress: ReceiveBufferSize: " +inSocket.getReceiveBufferSize());
            }


            // set up outbound socket
            // don't bind() or connect() so we can set src and dst addr and port later
            outSocket = new DatagramSocket();

            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            return new ApplicationResponse(false,  "Cannot open socket " + e.getMessage());
        }

        // set up timer to count throughput
        dataRateEvaluator = new TimerTask() {
                boolean running = true;
                // no per second
                int packetsPerSecond = 0;
                // volume per second
                long volumePerSecond = 0;

                @Override
				public void run() {
                    if (running) {
                        packetsPerSecond = count - lastTimeCount;
                        volumePerSecond = volume - lastTimeVolume;

                        lastTime = System.currentTimeMillis();
                        long elaspsedSecs = (lastTime - startTime)/1000;
                        long elaspsedMS = (lastTime - startTime)%1000;

                        usr.net.Address address = usr.router.RouterDirectory.getRouter().getAddress();
                        if (verbose) {
                            Logger.getLogger("log").logln(USR.STDOUT,
                                                      address + ": Ingress count: " + count + " time: " + elaspsedSecs + "." + elaspsedMS + " packetsPerSecond: "  + packetsPerSecond + " volumePerSecond: " + volumePerSecond);
                        }

                        lastTimeCount = count;
                        lastTimeVolume = volume;
                    }
                }

                @Override
				public boolean cancel() {
                    //Logger.getLogger("log").log(USR.STDOUT, "cancel @ " + count);

                    if (running) {
                        running = false;
                    }


                    return running;
                }

                @Override
				public long scheduledExecutionTime() {
                    //Logger.getLogger("log").log(USR.STDOUT, "scheduledExecutionTime:");
                    return 0;
                }

            };

        // if there is no timer, start one
        if (timer == null) {
            timer = new Timer();
            timer.schedule(dataRateEvaluator, 1000, 1000);
        }

        startTime = System.currentTimeMillis();


        running = true;

        return new ApplicationResponse(true, "");
    }


    /**
     * Stop the application
     */
    @Override
	public ApplicationResponse stop() {
        // interrupt select()
        Thread.currentThread().interrupt();

        running = false;

        if (inSocket != null) {
            inSocket.close();
        }

        if (outSocket != null) {
            outSocket.close();
        }


        Logger.getLogger("log").logln(USR.ERROR, "IngressS stop");

        return new ApplicationResponse(true, "");
    }


    /**
     * Run the Ingress application
     *
     */
    @Override
	public void run()  {
        ByteBuffer inDatagramBuffer = null;
        Datagram outDatagram = null;

        // The queue for packets
        LinkedList<ByteBuffer> queue = new LinkedList<ByteBuffer>();


        // Start Delay
        if (startDelay > 0) {
            try {
                Thread.sleep(startDelay);
            } catch (Exception e) {
            }
        }

        try {
            // setup Selector on DatagramSocket inSocket
            Selector selector = Selector.open();
            DatagramChannel channel = inSocket.getChannel();
            channel.register(selector, SelectionKey.OP_READ);

            int maxQueueSize = 0;

            // get dst address and port
            Address dstAddr = usrAddr.getAddress();
            int dstPort = usrAddr.getPort();


            while (running) {

                // test for a  packet, if select() returns > 0 there is something
                int readyChannels = 0;;


                if (queue.size() > 0) {
                    // there is something in the queue
                    // so check if another packet has arrived
                    // we need to process the queue if no packet is there
                    readyChannels = selector.selectNow();
                } else {
                    // there is nothing in the queue
                    // so wait until a packet arrives
                    readyChannels = selector.select();
                }

                if (readyChannels > 0) {

                    // read from socket and put on queue

                    // new space for each packet
                    ByteBuffer inBuffer = ByteBuffer.allocate(2048);

                    // read a UDP packet into the ByteBuffer
                    channel.receive(inBuffer);

                    // normalize ByteBuffer
                    inBuffer.flip();


                    int size = inBuffer.limit();

                    count++;
                    volume += size;

                    if (verbose) {
                        if (count % 100 == 0) {
                            Logger.getLogger("log").logln(USR.STDOUT, "IngressS RECV " + count + " recv: " +  inBuffer.limit() + " volume: " + volume + " maxQueueSize: " + maxQueueSize);
                            maxQueueSize = 0;
                        }
                    }

                    // add the ByteBuffer to the queue
                    queue.add(inBuffer);

                    if (queue.size() > maxQueueSize) {
                        maxQueueSize = queue.size();
                    }

                    // Patch up selectedKeys
                    // A complete wierdness of Java select.
                    // What chimp devised this.
                    selector.selectedKeys().clear();

                } else {
                    if (queue.size() == 0) {
                        Logger.getLogger("log").logln(USR.ERROR, "IngressS queue empty");
                        continue;
                    }

                    // the queue has something in it

                    //  collect from queue
                    inDatagramBuffer = queue.poll();


                    /*
                    if (count % 100 == 0) {
                        System.err.println("Ingress IN  count: " + count + " recv: " +  inDatagramBuffer.limit() + " volume: " + volume + " queue size: " + queue.size());
                    }
                    */

                    // now create a USR Datagram
                    // from original data
                    outDatagram = DatagramFactory.newDatagram(inDatagramBuffer);
                    // set the dst address and port
                    // the src addr and port will be set in send()
                    outDatagram.setDstAddress(dstAddr);
                    outDatagram.setDstPort(dstPort);


                    // and send
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
            }

            selector.close();

        } catch (Exception e) {
            Logger.getLogger("log").log(USR.ERROR, e.getMessage());
            e.printStackTrace();
        }



        //Logger.getLogger("log").logln(USR.STDOUT, "IngressS: end of run()");


    }



}
