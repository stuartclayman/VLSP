package usr.test;

import usr.net.*;
import usr.logging.*;
import usr.logging.*;
import usr.router.NetIF;
import usr.router.TCPNetIF;
import usr.router.NetIFListener;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubClient implements NetIFListener {
    final static int PORT_NUMBER = 4433;
    ConnectionOverTCP connection;
    NetIF netIF;
    Logger logger;

    public StubClient(String host, int port) {
        try {
            logger = Logger.getLogger("log");
            logger.addOutput(System.err, new BitMask(USR.ERROR));

	    // initialise socket
            TCPEndPointSrc src = new TCPEndPointSrc(host, port);

            netIF = new TCPNetIF(src);
            netIF.setAddress(new GIDAddress(1));
            netIF.setNetIFListener(this);
            netIF.connect();
            
            Logger.getLogger("log").logln(USR.ERROR, "StubClient: Connected to: " + host);


        } catch (UnknownHostException uhe) {
            Logger.getLogger("log").logln(USR.ERROR, "StubClient: Unknown host " + host);
            System.exit(1);
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR, "StubClient: Cannot connect to " + host + "on port " + port);
            System.exit(1);
        }
    }

    /**
     * Write stuff
     */
    void writeALot(int count) {
        Datagram datagram = null;

        for (int i = 0; i < count; i++) {
            String line = "line " + i;
            ByteBuffer buffer = ByteBuffer.allocate(line.length());
            buffer.put(line.getBytes());

            datagram = DatagramFactory.newDatagram(buffer);


            datagram.setDstAddress(new GIDAddress(47));
            datagram.setDstPort(3333);

            if (netIF.sendDatagram(datagram) == false) {
                return;
            } else {

                //Logger.getLogger("log").logln(USR.ERROR, "Sent: " + datagram + " with " + new String(datagram.getPayload()));
            }
        }

        netIF.close();
    }

    /**
     * A NetIF is closing.
     */
    public boolean netIFClosing(NetIF netIF) {
        return true;
    }

    /**
     * Can accept a Datagram
     */
    public boolean canAcceptDatagram(NetIF n) {
        return true;
    }

    /**
     * Can route a Datagram
     */
    public boolean canRoute(Datagram d) {
        return true;
    }

    /**
     * A NetIF has a datagram.
     */
    public boolean datagramArrived(NetIF netIF, Datagram datagram) {
        return true;
    }

    public static void main(String[] args) throws IOException {
        int count = 100;
        int port = PORT_NUMBER;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
        }

        if (args.length == 2) {
            // get no of writes
            Scanner scanner = new Scanner(args[1]);

            port = scanner.nextInt();
        }

        StubClient client = new StubClient("localhost",  port);
        client.writeALot(count);
    }


}

