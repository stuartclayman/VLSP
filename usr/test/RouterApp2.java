package usr.test;

import usr.router.Router;
import usr.router.AppSocket;
import usr.net.*;
import usr.interactor.RouterInteractor;
import usr.protocol.Protocol;
import java.util.Scanner;
import java.nio.ByteBuffer;

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
            router.setGlobalID(1);

            // now set up an AppSocket to receive
            recvSocket = new AppSocket(router, 5555);

            // now set up an AppSocket to send
            sendSocket = new AppSocket(router);

            // and we want to connect to GID 1 : port 5555
            sendSocket.connect(new GIDAddress(1), 5555);

        } catch (Exception e) {
            System.err.println("RouterApp2 exception: " + e);
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


            if (sendSocket.send(datagram) == false) {
                return;
            } else {

                //System.out.println("Sent: " + datagram + " with " + new String(datagram.getPayload()));
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
