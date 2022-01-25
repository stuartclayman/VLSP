package demo_usr.paths;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;


/**
 * This class gets data from a queue and
 * sends it to a UDP socket.
 */
public class UDPForwarder implements Callable <Object> {
    java.net.DatagramSocket outSocket;
    InetSocketAddress udpAddr;
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
    Timer timer = null;
    TimerTask dataRateEvaluator = null;
    long startTime = 0;
    long lastTime = 0;

    // verbose
    int verbose = 0;

    // Latch for end synchronization
    private final CountDownLatch actuallyFinishedLatch;

    
    public UDPForwarder(InetSocketAddress udpAddr, LinkedBlockingDeque<usr.net.Datagram> queue, int verb) throws Exception {
        // set up outbound socket
        // don't bind() or connect() so we can set src and dst addr and port
        outSocket = new java.net.DatagramSocket();

        // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());
        // The queue to put new Datagrams on
        this.queue = queue;

        // The USR address to forward to
        this.udpAddr = udpAddr;

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

                    @Override
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
                                                              address + ": UDPForwarder count: " + count + " time: " + elaspsedSecs + "." + elaspsedMS + " pkts/sec: "  + packetsPerSecond + " vol/sec: " + volumePerSecond + " Mbps: " + (((float)volumePerSecond) * 8 / (1024 * 1024)));
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

        }


        startTime = System.currentTimeMillis();

        running = true;

    }


    @Override
    public Object call() {
        Datagram inDatagram = null;
        java.net.DatagramPacket outDatagram = null;

        try {
            // get dst address and port
            InetAddress dstAddr = udpAddr.getAddress();
            int dstPort = udpAddr.getPort();

            while (running) {

                try {
                    do {

                        inDatagram = queue.take();
                        if (inDatagram == null) {
                            Logger.getLogger("log").logln(USR.ERROR, "UDPForwarder IN DatagramPacket is NULL " + count + " volume: " + volume + " at " + System.currentTimeMillis());
                        }
                    } while (inDatagram == null);
                } catch (InterruptedException ie) {
                    //Logger.getLogger("log").logln(USR.ERROR, "UDPForwarder interrupted");
                    running = false;
                    break;
                }


                count++;

                // original data
                byte[] data = inDatagram.getPayload();

                volume += data.length;

                if (verbose == 2) {
                    if (count % 100 == 0) {
                        Logger.getLogger("log").logln(USR.STDOUT, "UDPForwarder " + count + " recv: " + data.length +  " volume: " + volume);
                    }
                }

                int remaining = data.length;
                int offset = 0;

                // send data out in 1024 sized chunks
                // we do this because netcat/nc only uses this size
                int packetSize = data.length;   // 1024;
                
                int chunk = (data.length < packetSize ? data.length : packetSize);

                do {
                    // now create a UDP Datagram
                    byte[] udpBuffer = new byte[packetSize];
                    outDatagram = new java.net.DatagramPacket(udpBuffer, packetSize);

                    // copy in data
                    outDatagram.setData(data, offset, chunk);

                    // now work out where to send it

                    // set the dst address and port
                    // the src addr and port will be set in send()
                    outDatagram.setAddress(dstAddr);
                    outDatagram.setPort(dstPort);


                    try {
                        outSocket.send(outDatagram);

                    } catch (Exception e) {
                        if (outSocket.isClosed()) {
                            break;
                        } else {
                            Logger.getLogger("log").logln(USR.ERROR, "Cant send: " + outDatagram);
                        }
                    }

                    remaining -= chunk;
                    offset += chunk;
                    chunk = (remaining > packetSize ? packetSize : remaining);

                } while (remaining > 0);


            }

        } catch (Exception e) {
            Logger.getLogger("log").log(USR.ERROR, e.getMessage());
            e.printStackTrace();
        }


        if (dataRateEvaluator != null) {
            dataRateEvaluator.cancel();
        }

        if (outSocket != null) {
            outSocket.close();
        }


        actuallyFinishedLatch.countDown();

        //Logger.getLogger("log").logln(USR.ERROR, "UDPForwarder: end of call()");

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
