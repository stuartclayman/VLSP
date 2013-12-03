package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.logging.*;
import usr.router.AppSocket;
import usr.net.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.net.SocketException;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp2 {
    // the router
    Router router = null;
    RouterEnv routerEnv = null;

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

            routerEnv = new RouterEnv(port, r2r, "Router-1");
            router = routerEnv.getRouter();

            // check
            if (routerEnv.isActive()) {
            } else {
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
        routerEnv.stop();
    }

    public static void main(String[] args) {
        int count = 10;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
            scanner.close();
        }


        RouterApp2 app2 = new RouterApp2();

        app2.writeALot(count);

        app2.end();
    }

}