package usr.test;

import usr.net.*;
import usr.router.NetIF;
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

public class StubServer {
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


    public StubServer(int listenPort) throws IOException {
        normal = new BitSet();
        normal.set(1);
        error = new BitSet();
        error.set(2);

        // allocate a new logger
        logger = new Logger();
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
            netIF.connect();
            logger.log(error, "StubServer: Listening on port: " + listenPort + "\n");
        } catch (IOException ioe) {
            logger.log(error, "StubServer: Cannot listen on port: " + listenPort + "\n");
            throw ioe;
        }
    }

    /**
     * Read stuff
     */
    void readALot() throws IOException {
        Datagram datagram;

        Timer timer = new Timer();
        TimerTask task = new TimerTask() { 
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

        timer.schedule(task, 1000, 1000);

        long t0 = System.currentTimeMillis();

        while ((datagram = netIF.readDatagram()) != null) {
            // check if Protocol.CONTROL
            if (datagram.getProtocol() == Protocol.CONTROL) {
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

        long t1 = System.currentTimeMillis();

        long elapsed = t1 - t0;
        int secs = (int) elapsed / 1000;
        int millis = (int) elapsed % 1000;

        NumberFormat millisFormat = new DecimalFormat("000"); 
        logger.log(error, "elapsed[" + count + "] = " + secs + ":" + millisFormat.format(millis) + "\n");

        timer.cancel();

        netIF.close();
    }

    public static void main(String[] args) throws IOException {
        StubServer server = new StubServer(PORT_NUMBER);

        server.readALot();
    }
}
