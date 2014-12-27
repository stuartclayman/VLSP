package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.logging.*;
import usr.net.*;
import usr.interactor.RouterInteractor;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.net.SocketException;
import usr.applications.Application;
import usr.applications.ApplicationResponse;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1C {
    // the router
    RouterEnv routerEnv = null;
    Router router = null;

    /**
     * Construct a RouterApp1C
     */
    public RouterApp1C(String remHost, int remPort) {
        
        try {
            //AddressFactory.setClassForAddress("usr.net.IPV4Address");

            int port = 18181;
            int r2r = 18182;

            routerEnv = new RouterEnv(port, r2r, "Router-1");
            router = routerEnv.getRouter();

            // start
            if (routerEnv.isActive()) {

                // set up id
                router.setAddress(new GIDAddress(1)); // new IPV4Address("192.168.7.1"));  // 


                // connnect to the other router
                // first we tal kto my own ManagementConsole
                String address = InetAddress.getLocalHost().getHostAddress();
                RouterInteractor selfInteractor = new RouterInteractor(address, 18181);

                // then set up Router-to-Router data connection
                selfInteractor.createConnection(remHost + ":" + remPort, 20);

                // and stop talking to the ManagementConsole
                selfInteractor.quit();



            } else {
                routerEnv.stop();
            }
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "RouterApp1C exception: " + e);
            e.printStackTrace();
        }

    }

    public void go() {
        router.appStart("usr.test.RouterApp1C$WriteALot");

        try {
            Thread.sleep(5000);

            routerEnv.stop();
        } catch (Exception e) {
        }
    }


    public static void main(String[] args) throws Exception {
        //String host = InetAddress.getLocalHost().getHostAddress();
        String host = "localhost";
        int count = 10;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
            scanner.close();
        } else if (args.length == 2) {
            host = args[0];

            // get no of writes
            Scanner scanner = new Scanner(args[1]);

            count = scanner.nextInt();
            scanner.close();
        }


        RouterApp1C app1c = new RouterApp1C(host, 18191);

        app1c.go();

    }


    public static class WriteALot implements Application {

        // the socket
        DatagramSocket socket;

        int count = 100;

        public WriteALot() {}

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
                // now set up a socket to send
                socket = new DatagramSocket();

                // and we want to connect to address 2 : port 3000
                socket.connect(  /* new IPV4Address("192.168.7.2") */ new GIDAddress(2) , 3000);

                return new ApplicationResponse(true, "start");

            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, "RouterApp1C exception: " + e);
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
         * Write stuff
         */
        public void run() {
            // check if there is a socket
            if (socket == null) {
                System.err.println("No socket");
                return;
            }


            Datagram datagram = null;

            try {
                Thread.sleep(4000);
                Logger.getLogger("log").logln(USR.STDOUT, "End of sleep");
            } catch (InterruptedException ie) {
            }

            for (int i = 0; i < count; i++) {
                String line = "line " + i;
                ByteBuffer buffer = ByteBuffer.allocate(line.length());
                buffer.put(line.getBytes());

                datagram = DatagramFactory.newDatagram(buffer);


                try {
                    socket.send(datagram);

                    //Logger.getLogger("log").logln(USR.STDOUT, "sent");

                } catch (SocketException se) {
                    se.printStackTrace();
                    //Logger.getLogger("log").logln(USR.STDOUT, "Sent: " + datagram + " with " + new String(datagram.getPayload()));
                }

            }

            Logger.getLogger("log").logln(USR.STDOUT, "ending....");

            socket.close();
        }




    }


}
