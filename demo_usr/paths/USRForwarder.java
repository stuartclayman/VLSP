package demo_usr.paths;

import usr.net.*;
import usr.logging.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.Timer;
import java.util.TimerTask;


/**
 * This class gets data from a queue and
 * sends it to a USR socket.
 */
public class USRForwarder implements Callable {
    usr.net.DatagramSocket outSocket;
    SocketAddress usrAddr;
    LinkedBlockingDeque<usr.net.Datagram> queue;
    boolean running = false;

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

    // verbose 
    int verbose = 0;

    // Latch for end synchronization
    private final CountDownLatch actuallyFinishedLatch;


    public USRForwarder(SocketAddress usrAddr, LinkedBlockingDeque<usr.net.Datagram> queue, int verb) throws Exception {
        // the socket to write to
        // set up outbound socket
        // don't bind() or connect() so we can set src and dst addr and port
        outSocket = new usr.net.DatagramSocket();

        // The queue to put new DatagramPackets on
        this.queue = queue;

        // The USR address to forward to
        this.usrAddr = usrAddr;

        // verbose
        this.verbose = verb;

        actuallyFinishedLatch = new CountDownLatch(1);

        /*
         * Start a TimerTask if this node is verbose.
         */
        if (verbose > 0) {

            // set up timer to count throughput
            dataRateEvaluator = new TimerTask() {
                    boolean running = true;
                    // no per second
                    int packetsPerSecond = 0;
                    // volume per second
                    long volumePerSecond = 0;

                    public void run() {
                        if (running) {
                            packetsPerSecond = count - lastTimeCount;
                            volumePerSecond = volume - lastTimeVolume;

                            lastTime = System.currentTimeMillis();
                            long elaspsedSecs = (lastTime - startTime)/1000;
                            long elaspsedMS = (lastTime - startTime)%1000;

                            usr.net.Address address = usr.router.RouterDirectory.getRouter().getAddress();
                            if (verbose > 0) {
                                Logger.getLogger("log").logln(USR.STDOUT,
                                                              address + ": USRForwarder count: " + count + " time: " + elaspsedSecs + "." + elaspsedMS + " pkts/sec: "  + packetsPerSecond + " vol/sec: " + volumePerSecond + " Mbps: " + (((float)volumePerSecond) * 8 / (1024 * 1024)));
                            }

                            lastTimeCount = count;
                            lastTimeVolume = volume;
                        }
                    }

                    public boolean cancel() {
                        //Logger.getLogger("log").log(USR.STDOUT, "cancel @ " + count);

                        if (running) {
                            running = false;
                        }


                        return running;
                    }

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

        }

        startTime = System.currentTimeMillis();


        running = true;
    }


    public Object call() {
        Datagram inDatagram = null;
        Datagram outDatagram = null;

        try {
            // get dst address and port
            Address dstAddr = usrAddr.getAddress();
            int dstPort = usrAddr.getPort();
                

            while (running) {
                // read a USR packet
                //inDatagram = queue.take();

                try {
                    do {
                        inDatagram = queue.take();
                        if (inDatagram == null) {
                            Logger.getLogger("log").logln(USR.ERROR, "USRForwarder IN DatagramPacket is NULL " + count + " volume: " + volume + " at " + System.currentTimeMillis());
                        }
                    } while (inDatagram == null);
                } catch (InterruptedException ie) {
                    //Logger.getLogger("log").logln(USR.ERROR, "USRForwarder interrupted");
                    running = false;
                    break;
                }

                count++;
                volume += inDatagram.getLength();

                if (verbose == 2) {
                    if (count % 100 == 0) {
                        System.err.println("USRForwarder IN  " + count + " recv: " +  inDatagram.getLength() + " volume: " + volume);
                    }
                }

                outDatagram = inDatagram;
                int outDataLength = outDatagram.getLength();
                //System.err.println("USRForwarder OUT " + count + " recv: " +  outDataLength + " volume: " + volume + " at " + System.currentTimeMillis());

                // set the dst address and port
                // the src addr and port will be set in send()
                outDatagram.setDstAddress(dstAddr);
                outDatagram.setDstPort(dstPort);

                try {
                    outSocket.send(outDatagram);

                    /*
                     * Inter Packet Delay not supported
                    // Inter Packet Delay 
                    if (interPacketDelay > 0) {
                        Thread.sleep(interPacketDelay);
                    }
                    */

                } catch (Exception e) {
                    if (outSocket.isClosed()) {
                        break;
                    } else {
                        Logger.getLogger("log").logln(USR.ERROR, "Cant send: " + outDatagram);
                    }
                }

                long t2 = System.currentTimeMillis();

            }

        } catch (Exception e) {
            Logger.getLogger("log").log(USR.ERROR, e.getMessage());
            e.printStackTrace();
        }

        dataRateEvaluator.cancel();

        if (outSocket != null) {
            outSocket.close();
        }


        actuallyFinishedLatch.countDown();

        // Logger.getLogger("log").logln(USR.ERROR, "USRForwarder: end of call()");

        return null;

    }

    /**
     * Wait for this Forwarder to terminate.
     */
    public void await() {
        try {
            actuallyFinishedLatch.await();
        } catch (InterruptedException ie) {
        }
    }



}
