package usr.test;

import usr.router.Router;
import usr.router.AppSocket;
import usr.net.GIDAddress;
import usr.net.Datagram;
import usr.net.GIDDatagram;
import java.util.Scanner;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1S {
    // the Router
    Router router = null;

    // the socket
    AppSocket socket;

    int count = 0;

    public RouterApp1S() {
        try {
            int port = 18191;
            int r2r = 18192;

            router = new Router(port, r2r, "Router-2");

            // start
            if (router.start()) {
            } else {
                router.stop();
            }

            // set ID
            router.setGlobalID(2);
        
            // now set up an AppSocket to receive
            socket = new AppSocket(router, 3000);

        } catch (Exception e) {
            System.err.println("RouterApp1S exception: " + e);
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
