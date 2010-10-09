package usr.test;

import usr.net.*;
import usr.logging.*;
import usr.router.NetIF;
import usr.router.TCPNetIF;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubClient {
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

