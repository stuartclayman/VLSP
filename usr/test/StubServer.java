package usr.test;

import usr.net.*;
import usr.logging.*;
import usr.router.NetIF;
import usr.router.NetIFListener;
import usr.router.TCPNetIF;
import usr.protocol.Protocol;
import usr.router.*;
import usr.router.DatagramDevice;
import usr.logging.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.Timer;
import java.util.TimerTask;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubServer extends MinimalDatagramDevice implements NetIFListener  {
    final static int PORT_NUMBER = 4433;

    static BitMask normal;
    static BitMask error;

    TCPNetIF netIF;
    ConnectionOverTCP connection;
    ServerSocket serverSocket;
    // and channel
    ServerSocketChannel channel;
    Logger logger;

    // total no of Datagrams in
    int count = 0;
    // last time count
    int lastTimeCount = 0;
    // no per second
    int diffs = 0;
    // start time
    long t0 = 0;
    Address addr;
    // Timer stuff
    Timer timer;
    TimerTask timerTask;
    int listenPort = 0;
    Object endObject_ = null;

    public StubServer(int lp) throws IOException {
        super("StubSeverver Fabric");
        listenPort = lp;
        addr = new GIDAddress(555);
        setAddress(addr);  // Dummy address for us
    }

    void run() {
        endObject_ = new Object();
        start();  //  Starts Minimal datagram device
        normal = new BitMask(USR.STDOUT);
        error = new BitMask(USR.ERROR);
        // allocate a new logger
        logger = Logger.getLogger("log");
        // tell it to output to stdout
        // and tell it what to pick up
        // it will actually output things where the log has bit 1 set
        logger.addOutput(System.out, normal);
        // tell it to output to stderr
        // and tell it what to pick up
        // it will actually output things where the log has bit 2 set
        logger.addOutput(System.err, error);


        // initialise the socket
        try {
            channel = ServerSocketChannel.open();
            serverSocket = channel.socket();
            serverSocket.bind(new InetSocketAddress(listenPort));

            TCPEndPointDst dst = new TCPEndPointDst(serverSocket);

            netIF = new TCPNetIF(dst, this);
            netIF.setName("TCPNetIF");
            netIF.setRemoteRouterAddress(new GIDAddress(1));
            netIF.connect();
            logger.log(error, "StubServer: Listening on port: " + listenPort + "\n");
        } catch (IOException ioe) {
            logger.log(error, "StubServer: Cannot listen on port: " + listenPort + "\n");
            return;
        }

        // set up timer to count throughput
        timerTask = new TimerTask() {
            boolean running = true;

            public void run() {
                if (running) {
                    diffs = count - lastTimeCount;
                    Logger.getLogger("log").logln(USR.ERROR, "Task count: " + count + " diff: "  + diffs);
                    lastTimeCount = count;
                }
            }

            public boolean cancel() {
                logger.log(error, "cancel @ " + count);

                if (running) {
                    running = false;
                }


                return running;
            }

            public long scheduledExecutionTime() {
                logger.log(error, "scheduledExecutionTime:");
                return 0;
            }

        };
        synchronized (endObject_) {
            try {
                endObject_.wait();
            } catch (InterruptedException e) {

            }
        }
        stop();
    }

    /**
     * Route either outbound down the interface or to ourselves.
     */
    public FabricDevice getRouteFabric(Datagram d) {
        if (ourAddress(d.getDstAddress())) {
            return getFabricDevice();
        }
        System.err.println("Received datagram to go the other way");
        return netIF.getFabricDevice();
    }

    /**
     * A NetIF has a datagram.
     */
    public synchronized boolean outQueueHandler(Datagram datagram, DatagramDevice dd) {
        // if there is no timer, start one
        if (timer == null) {
            timer = new Timer();
            timer.schedule(timerTask, 1000, 1000);
            t0 = System.currentTimeMillis();
        }

        // check if Protocol.CONTROL
        if (datagram.getProtocol() == Protocol.CONTROL) {
            synchronized (endObject_) {
                endObject_.notify();
            }
            return true;
        }

        logger.log(normal, count + ". ");
        logger.log(normal,
                   "HL: " + datagram.getHeaderLength() +
                   " TL: " + datagram.getTotalLength() +
                   " From: " + datagram.getSrcAddress() +
                   " To: " + datagram.getDstAddress() +
                   ". ");
        byte[] payload = datagram.getPayload();

        if (payload == null) {
            logger.logln(normal, "No payload");
        } else {
            logger.logln(normal, new String(payload));
        }

        count++;

        return true;
    }

    /** Deal with TTL expire */
    public void TTLDrop(Datagram dg) {
    }

    /** A datagram device has closed and must be removed */
    public void closedDevice(DatagramDevice dd) {

    }

    /**
     * A NetIF is closing.
     */
    public void stop() {
        super.stop();
        long t1 = System.currentTimeMillis();

        long elapsed = t1 - t0;
        int secs = (int)elapsed / 1000;
        int millis = (int)elapsed % 1000;

        NumberFormat millisFormat = new DecimalFormat("000");
        logger.log(error, "elapsed[" + count + "] = " + secs + ":" + millisFormat.format(millis) + "\n");

        netIF.remoteClose();

        if (timer != null) {
            timer.cancel();
        }

        timer = null;

    }

    public static void main(String[] args) throws IOException {
        StubServer server = new StubServer(PORT_NUMBER);

        server.run();
    }

}