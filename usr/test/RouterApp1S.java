package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.logging.*;
import usr.net.*;
import java.net.SocketException;
import usr.applications.Application;
import usr.applications.ApplicationResponse;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1S {
    // the Router
    Router router = null;
    RouterEnv routerEnv = null;

    public RouterApp1S() {
        try {
            //AddressFactory.setClassForAddress("usr.net.IPV4Address");

            int port = 18191;
            int r2r = 18192;

            routerEnv = new RouterEnv(port, r2r, "Router-2");
            router = routerEnv.getRouter();

            // check
            if (routerEnv.isActive()) {
                // set ID
                router.setAddress(new GIDAddress(2));  // new IPV4Address("192.168.7.2")); // 

            } else {
                routerEnv.stop();
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "RouterApp1S exception: " + e);
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        RouterApp1S app1s = new RouterApp1S();

        app1s.go();

    }

    public void go() {
        router.appStart("usr.test.RouterApp1S$ReadALot");
    }


    public static class ReadALot implements Application {
        // the socket
        DatagramSocket socket;

        int count = 0;


        public ReadALot() {}

        /**
         * Initialize with some args
         */
        public ApplicationResponse init(String[] args) {
            return new ApplicationResponse(true, "init");
        }

        /**
         * Start an application.
         * This is called before run().
         */
        public ApplicationResponse start() {
            try {
                // now set up a socket to receive
                socket = new DatagramSocket(3000);

                return new ApplicationResponse(true, "start");

            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, "RouterApp1S exception: " + e);
                e.printStackTrace();
                return new ApplicationResponse(false, "start");

            }


        }


        /**
         * Stop an application.
         * This is called to implement graceful shut down
         * and cause run() to end.
         */
        public ApplicationResponse stop() {
            socket.close();

            return new ApplicationResponse(true, "stop");
        }


        /**
         * Read stuff
         */
        public void run() {
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
    }


}
