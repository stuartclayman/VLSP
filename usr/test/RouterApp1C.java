package usr.test;

import usr.router.Router;
import usr.router.AppSocket;
import usr.net.*;
import usr.interactor.RouterInteractor;
import java.util.Scanner;
import java.nio.ByteBuffer;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1C {
    // the router
    Router router = null;

    // the socket
    AppSocket socket;

    /**
     * Construct a RouterApp1C
     */
    public RouterApp1C(String remHost, int remPort) {
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

            // connnect to the other router
            // first we tal kto my own ManagementConsole
            RouterInteractor selfInteractor = new RouterInteractor("localhost", 18181);

            // then set up Router-to-Router data connection
            selfInteractor.createConnection(remHost + ":" + remPort, 20);

            // and stop talking to the ManagementConsole
            selfInteractor.quit();

            // now set up an AppSocket to send
            socket = new AppSocket(router);

            // and we want to connect to GID 2 : port 3000
            socket.connect(new GIDAddress(2), 3000);

        } catch (Exception e) {
            System.err.println("RouterApp1C exception: " + e);
            e.printStackTrace();
        }
            
    }


    /** 
     * Write stuff
     */
    void writeALot(int count) {
        Datagram datagram = null;

        try { 
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
        }

        for (int i = 0; i < count; i++) {
            String line = "line " + i;
            ByteBuffer buffer = ByteBuffer.allocate(line.length());
            buffer.put(line.getBytes());

            datagram = DatagramFactory.newDatagram(buffer);


            if (socket.send(datagram) == false) {
                return;
            } else {

                //System.out.println("Sent: " + datagram + " with " + new String(datagram.getPayload()));
            }

        }

        System.out.println("ending....");

        try { 
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
        }

        end();
    }

    void end() {
        router.stop();
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
