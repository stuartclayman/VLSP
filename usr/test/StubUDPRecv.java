package usr.test;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.ConnectionOverUDP;
import usr.net.Datagram;
import usr.net.UDPEndPointDst;

public class StubUDPRecv {
    final static int PORT_NUMBER = 14433;
    ConnectionOverUDP connection;
    java.net.DatagramSocket recvSocket;

    public StubUDPRecv(int listenPort) throws IOException {
        // initialise the socket
        try {
            recvSocket = new java.net.DatagramSocket(listenPort);
            UDPEndPointDst dst = new UDPEndPointDst(recvSocket);

            connection = new ConnectionOverUDP(dst);
            connection.connect();

            Logger.getLogger("log").logln(USR.ERROR, "StubUDPRecv: Listening on port: " + PORT_NUMBER);
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, "StubUDPRecv: Cannot listen on port: " + PORT_NUMBER);
            throw ioe;
        }
    }

    /**
     * Read stuff
     */
    void readALot() throws IOException {
        Datagram datagram;
        int count = 0;

        long t0 = System.currentTimeMillis();

        while ((datagram = connection.readDatagram()) != null) {
            System.out.print(count + ". ");
            System.out.print("HL: " + datagram.getHeaderLength() +
                             " TL: " + datagram.getTotalLength() +
                             " From: " + datagram.getSrcAddress() +
                             " To: " + datagram.getDstAddress() +
                             ". ");
            byte[] payload = datagram.getPayload();

            if (payload == null) {
                Logger.getLogger("log").logln(USR.STDOUT, "No payload");
            } else {
                Logger.getLogger("log").logln(USR.STDOUT, new String(payload));
            }

            count++;
        }

        long t1 = System.currentTimeMillis();

        long elapsed = t1 - t0;
        int secs = (int)elapsed / 1000;
        int millis = (int)elapsed % 1000;

        NumberFormat millisFormat = new DecimalFormat("000");
        Logger.getLogger("log").logln(USR.ERROR, "elapsed[" + count + "] = " + secs + ":" + millisFormat.format(millis));

    }

    public static void main(String[] args) throws IOException {
        StubUDPRecv server = new StubUDPRecv(PORT_NUMBER);

        while (true) {
            server.readALot();
        }
    }

}
