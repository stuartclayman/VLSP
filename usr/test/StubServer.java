package usr.test;

import usr.net.*;
import usr.router.NetIF;
import usr.router.NetIFListener;
import usr.router.TCPNetIF;
import usr.protocol.Protocol;
import usr.logging.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubServer implements NetIFListener {
    final static int PORT_NUMBER = 4433;

    static BitSet normal;
    static BitSet error;

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

    // Timer stuff
    Timer timer;
    TimerTask timerTask;

    public StubServer(int listenPort) throws IOException {
        normal = new BitSet();
        normal.set(1);
        error = new BitSet();
        error.set(2);

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

            netIF = new TCPNetIF(dst);
            netIF.setNetIFListener(this);
            netIF.connect();
            logger.log(error, "StubServer: Listening on port: " + listenPort + "\n");
        } catch (IOException ioe) {
            logger.log(error, "StubServer: Cannot listen on port: " + listenPort + "\n");
            throw ioe;
        }

        // set up timer to count throughput
        timerTask = new TimerTask() { 
                boolean running = true;

                public void run() {
                    if (running) {
                        diffs = count - lastTimeCount;
                        System.err.println("Task count: " + count + " diff: "  + diffs);
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
    }

    /**
     * A NetIF has a datagram.
     */
    public boolean datagramArrived(NetIF netIF, Datagram datagram) {
        // if there is no timer, start one
        if (timer == null) {
            timer = new Timer();
            timer.schedule(timerTask, 1000, 1000);
            t0 = System.currentTimeMillis();
        }

        // check if Protocol.CONTROL
        if (datagram.getProtocol() == Protocol.CONTROL) {
            this.netIFClosing(netIF);
            return false;
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

    /**
     * A NetIF is closing.
     */
    public boolean netIFClosing(NetIF netIF) {
        long t1 = System.currentTimeMillis();

        long elapsed = t1 - t0;
        int secs = (int) elapsed / 1000;
        int millis = (int) elapsed % 1000;

        NumberFormat millisFormat = new DecimalFormat("000"); 
        logger.log(error, "elapsed[" + count + "] = " + secs + ":" + millisFormat.format(millis) + "\n");

        netIF.close();

        timer.cancel();

        timer = null;

        return true;
    }




    public static void main(String[] args) throws IOException {
        StubServer server = new StubServer(PORT_NUMBER);
    }
}
