package usr.test;

import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Scanner;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.GIDAddress;
import usr.router.AppSocket;
import usr.router.Router;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp2 {
    // the router
    Router router = null;

    // the recv socket
    AppSocket recvSocket;

    // the send socket
    AppSocket sendSocket;

    /**
     * Construct a RouterApp2
     */
    public RouterApp2() {
        try {
            int port = 18181;
            int r2r = 18182;

            router = new Router(port, r2r, "Router-1");

            // start
            if (router.start()) {
            } else {
                router.stop();
            }

            // set up id
            router.setAddress(new GIDAddress(1));

            // now set up an AppSocket to receive
            recvSocket = new AppSocket(router, 5555);

            // now set up an AppSocket to send
            sendSocket = new AppSocket(router);

            // and we want to connect to address 1 : port 5555
            sendSocket.connect(new GIDAddress(1), 5555);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "RouterApp2 exception: " + e);
            e.printStackTrace();
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


            try {
                sendSocket.send(datagram);
                //Logger.getLogger("log").logln(USR.STDOUT, "Sent: " + datagram + " with " + new String(datagram.getPayload()));
            } catch (SocketException se) {
                return;
            }
        }
    }

    void end() {
        router.stop();
    }

    public static void main(String[] args) {
        int count = 10;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
        }


        RouterApp2 app2 = new RouterApp2();

        app2.writeALot(count);

        app2.end();
    }

}