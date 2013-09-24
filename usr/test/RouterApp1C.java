package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.logging.*;
import usr.net.*;
import usr.interactor.RouterInteractor;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.net.SocketException;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1C {
    // the router
    RouterEnv routerEnv = null;
    Router router = null;

    // the socket
    DatagramSocket socket;

    /**
     * Construct a RouterApp1C
     */
    public RouterApp1C(String remHost, int remPort) {
        try {
            AddressFactory.setClassForAddress("usr.net.IPV4Address");

            int port = 18181;
            int r2r = 18182;

            routerEnv = new RouterEnv(port, r2r, "Router-1");
            router = routerEnv.getRouter();

            // start
            if (routerEnv.isActive()) {

                // set up id
                router.setAddress(new IPV4Address("192.168.7.1"));  // WAS new GIDAddress(1));

                // connnect to the other router
                // first we tal kto my own ManagementConsole
                RouterInteractor selfInteractor = new RouterInteractor("localhost", 18181);

                // then set up Router-to-Router data connection
                selfInteractor.createConnection(remHost + ":" + remPort, 20);

                // and stop talking to the ManagementConsole
                selfInteractor.quit();

                // now set up a socket to send
                socket = new DatagramSocket();

                // and we want to connect to address 2 : port 3000
                socket.connect(  new IPV4Address("192.168.7.2") /* new GIDAddress(2)  */, 3000);

            } else {
                routerEnv.stop();
            }
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "RouterApp1C exception: " + e);
            e.printStackTrace();
        }

    }

    /**
     * Write stuff
     */
    void writeALot(int count) {
        // check if there is a socket
        if (socket == null) {
            return;
        }


        Datagram datagram = null;

        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
        }

        for (int i = 0; i < count; i++) {
            String line = "line " + i;
            ByteBuffer buffer = ByteBuffer.allocate(line.length());
            buffer.put(line.getBytes());

            datagram = DatagramFactory.newDatagram(buffer);


            try {
                socket.send(datagram);
            } catch (SocketException se) {
                //Logger.getLogger("log").logln(USR.STDOUT, "Sent: " + datagram + " with " + new String(datagram.getPayload()));
            }

        }

        Logger.getLogger("log").logln(USR.STDOUT, "ending....");

        socket.close();

        end();
    }

    void end() {
        routerEnv.stop();
    }

    public static void main(String[] args) {
        String host = "localhost";
        int count = 10;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
        } else if (args.length == 2) {
            host = args[0];

            // get no of writes
            Scanner scanner = new Scanner(args[1]);

            count = scanner.nextInt();
        }


        RouterApp1C app1c = new RouterApp1C(host, 18191);

        app1c.writeALot(count);

    }

}
