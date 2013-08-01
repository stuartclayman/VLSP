package usr.test;

import usr.net.*;
import usr.logging.*;
import usr.router.NetIF;
import usr.router.TCPNetIF;
import usr.router.DatagramDevice;
import usr.protocol.Protocol;
import usr.logging.*;
import usr.router.FabricDevice;
import usr.router.NetIFListener;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.Timer;
import java.util.TimerTask;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StubServer2 implements NetIFListener {
    final static int PORT_NUMBER = 4433;

    static BitMask normal;
    static BitMask error;
    boolean running = true;
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
    LinkedBlockingQueue<Datagram> datagramQueue_;


    public StubServer2(int listenPort) throws IOException {
        normal = new BitMask();
        normal.set(1);
        error = new BitMask();
        error.set(2);

        // allocate a new logger
        logger = new Logger("log");
        // tell it to output to stdout
        // and tell it what to pick up
        // it will actually output things where the log has bit 1 set
        logger.addOutput(System.out, normal);
        // tell it to output to stderr
        // and tell it what to pick up
        // it will actually output things where the log has bit 2 set
        logger.addOutput(System.err, error);
        datagramQueue_ = new  LinkedBlockingQueue<Datagram> ();



        // initialise the socket
        try {
            channel = ServerSocketChannel.open();
            serverSocket = channel.socket();
            serverSocket.bind(new InetSocketAddress(listenPort));

            TCPEndPointDst dst = new TCPEndPointDst(serverSocket);

            netIF = new TCPNetIF(dst, this);
            netIF.connect();
            logger.log(error, "StubServer: Listening on port: " + listenPort + "\n");
        } catch (IOException ioe) {
            logger.log(error, "StubServer: Cannot listen on port: " + listenPort + "\n");
            throw ioe;
        }
    }

    /**
     * Can route a Datagram
     */
    public NetIF getRoute(Datagram d) {
        return null;
    }

    /**
     * Fake interface
     */
    public FabricDevice getRouteFabric(Datagram d) throws NoRouteToHostException {
        throw new NoRouteToHostException();
    }

    /** Accept all traffic*/
    public boolean ourAddress(Address a) {
        return true;
    }

    /**
     * get name
     */
    public String getName() {
        return netIF.getName();
    }

    public synchronized boolean datagramArrived(NetIF netIF, Datagram datagram) {
        datagramQueue_.add(datagram);
        notifyAll();
        return true;
    }

    /** Deal with TTL expire */
    public void TTLDrop(Datagram dg) {
    }

    /** A datagram device has closed and must be removed */
    public void closedDevice(DatagramDevice dd) {

    }

    /**
     * Read stuff
     */
    void readALot() throws IOException {
        Datagram datagram;

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {


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

        timer.schedule(task, 1000, 1000);

        long t0 = System.currentTimeMillis();
        synchronized (this) {
            while (running || datagramQueue_.size() > 0) {
                // check if Protocol.CONTROL
                datagram = datagramQueue_.poll();

                if (datagram == null) {
                    try {
                        wait();
                    } catch (java.lang.InterruptedException e) {

                    }

                    continue;
                }

                if (datagram.getProtocol() == Protocol.CONTROL) {
                    Logger.getLogger("log").logln(USR.STDOUT, "Got control packet");
                    break;
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
                    logger.log(normal, "No payload");
                } else {
                    logger.log(normal, new String(payload));
                }
                logger.log(normal, "\n");

                count++;
            }
        }
        long t1 = System.currentTimeMillis();

        long elapsed = t1 - t0;
        int secs = (int)elapsed / 1000;
        int millis = (int)elapsed % 1000;

        NumberFormat millisFormat = new DecimalFormat("000");
        logger.log(error, "elapsed[" + count + "] = " + secs + ":" + millisFormat.format(millis) + "\n");

        timer.cancel();

        netIF.close();
    }

    public static void main(String[] args) throws IOException {
        StubServer2 server = new StubServer2(PORT_NUMBER);

        server.readALot();
    }

}