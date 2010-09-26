package usr.test;

import usr.router.Router;
import usr.net.*;
import usr.interactor.RouterInteractor;
import usr.protocol.Protocol;
import java.util.Scanner;
import java.nio.ByteBuffer;

/**
 * Test Router startup and simple DatagramSocket.
 */
public class RouterApp2DS {
    // the router
    Router router = null;

    // the recv socket
    DatagramSocket recvSocket;

    // the send socket
    DatagramSocket sendSocket;

    /**
     * Construct a RouterApp2DS
     */
    public RouterApp2DS() {
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

            // now set up an DatagramSocket to receive
            recvSocket = new DatagramSocket(5555);

            // now set up an DatagramSocket to send
            sendSocket = new DatagramSocket();

            // and we want to connect to GID 1 : port 5555
            sendSocket.connect(new GIDAddress(1), 5555);

        } catch (Exception e) {
            System.err.println("RouterApp2DS exception: " + e);
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

            datagram = DatagramFactory.newDatagram(Protocol.DATA, buffer);


            if (sendSocket.send(datagram) == false) {
                return;
            } else {

                //System.out.println("Sent: " + datagram + " with " + new String(datagram.getPayload()));
            }


            try { 
                Thread.sleep(20);
            } catch (InterruptedException ie) {
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


        RouterApp2DS app2 = new RouterApp2DS();

        app2.writeALot(count);

        app2.end();
    }



}
