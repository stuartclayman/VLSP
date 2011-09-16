package usr.test;

import usr.router.Router;
import usr.logging.*;
import usr.net.*;
import java.util.Scanner;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1S {
    // the Router
    Router router = null;

    // the socket
    DatagramSocket socket;

    int count = 0;

    public RouterApp1S() {
        try {
            AddressFactory.setClassForAddress("usr.net.IPV4Address");

            int port = 18191;
            int r2r = 18192;

            router = new Router(port, r2r, "Router-2");

            // start
            if (router.start()) {
                // set ID
                router.setAddress(new IPV4Address("192.168.7.2")); // WAS new GIDAddress(2));

                // set a thread context, so the DatagramSocket can find the router
                router.addThreadContext(Thread.currentThread());

                // now set up a socket to receive
                socket = new DatagramSocket(3000);

            } else {
                router.stop();
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "RouterApp1S exception: " + e);
            e.printStackTrace();
        }


    }


    /**
     * Read stuff
     */
    void readALot() {
        Datagram datagram;

        while ((datagram = socket.receive()) != null) {

            System.err.print(count + ". ");
            System.err.print("HL: " + datagram.getHeaderLength() +
                             " TL: " + datagram.getTotalLength() +
                             " From: " + datagram.getSrcAddress() +
                             " To: " + datagram.getDstAddress() +
                             ". ");
            byte[] payload = datagram.getPayload();

            if (payload == null) {
                System.err.print("No payload");
            } else {
                System.err.print(new String(payload));
            }
            System.err.print("\n");

            count++;
        }

    }

    public static void main(String[] args) {
        RouterApp1S app1s = new RouterApp1S();

        app1s.readALot();
    }


}
