package usr.test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.ConnectionOverUDP;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.IPV4Address;
import usr.net.UDPEndPointSrc;

public class StubUDPSend {
    final static int PORT_NUMBER = 14433;
    ConnectionOverUDP connection;

    public StubUDPSend(String host, int port) {
        try {
            // initialise socket
            UDPEndPointSrc src = new UDPEndPointSrc(host, port);
            connection = new ConnectionOverUDP(src);
            connection.setAddress(new IPV4Address("localhost"));

            // TODO: fix bugs with no connect !! connection.connect();
            Logger.getLogger("log").logln(USR.ERROR, "Connected to: " + host);

        } catch (UnknownHostException uhe) {
            Logger.getLogger("log").logln(USR.ERROR, "StubUDPSend: Unknown host " + host);
            System.exit(1);
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR, "StubUDPSend: Cannot connect to " + host + "on port " + port);
            System.exit(1);
        }
    }

    /**
     * Write stuff
     */
    void writeALot(int count) throws IOException {
        Datagram datagram;

        for (int i = 0; i < count; i++) {
            String line = "line " + i;
            ByteBuffer buffer = ByteBuffer.allocate(line.length());
            buffer.put(line.getBytes());
            //// ORIG datagram = new IPV4Datagram(buffer);
            datagram = DatagramFactory.newDatagram(buffer);

            String addr = "192.168.7.1";
            try {
                datagram.setDstAddress(new IPV4Address(addr));
            } catch (UnknownHostException uhe) {
                Logger.getLogger("log").logln(USR.ERROR, "UnknownHostException " + addr);
            }

            if (connection.sendDatagram(datagram) == false) {
                return;
            } else {

                /*
                   try {
                    Thread.sleep(100);
                   } catch (InterruptedException ie) {
                   }
                 */

                Logger.getLogger("log").logln(USR.STDOUT, "Sent: " + datagram + " with " + new String(datagram.getPayload()));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int count = 100;
        int port = PORT_NUMBER;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
            scanner.close();
        }

        if (args.length == 2) {
            // get no of writes
            Scanner scanner = new Scanner(args[1]);

            port = scanner.nextInt();
            scanner.close();
        }

        StubUDPSend client = new StubUDPSend("localhost", port);
        client.writeALot(count);
    }

}
