package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.logging.*;
import usr.net.*;
import java.util.Scanner;
import java.net.SocketException;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1S {
    // the Router
    Router router = null;
    RouterEnv routerEnv = null;

    // the socket
    DatagramSocket socket;

    int count = 0;

    public RouterApp1S() {
        try {
            AddressFactory.setClassForAddress("usr.net.IPV4Address");

            int port = 18191;
            int r2r = 18192;

            routerEnv = new RouterEnv(port, r2r, "Router-2");
            router = routerEnv.getRouter();

            // check
            if (routerEnv.isActive()) {
                // set ID
                router.setAddress(new IPV4Address("192.168.7.2")); // WAS new GIDAddress(2));

                // now set up a socket to receive
                socket = new DatagramSocket(3000);

            } else {
                routerEnv.stop();
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

        try {
            while ((datagram = socket.receive()) != null) {

                System.out.print(count + ". ");
                System.out.print("HL: " + datagram.getHeaderLength() +
                                 " TL: " + datagram.getTotalLength() +
                                 " From: " + datagram.getSrcAddress() +
                                 " To: " + datagram.getDstAddress() +
                                 ". ");
                byte[] payload = datagram.getPayload();

                if (payload == null) {
                    System.out.print("No payload");
                } else {
                    System.out.print(new String(payload));
                }
                System.out.print("\n");

                count++;
            }

        } catch (SocketException se) {
            System.err.println(se.getMessage());
        }
    }

    public static void main(String[] args) {
        RouterApp1S app1s = new RouterApp1S();

        app1s.readALot();
    }

}
