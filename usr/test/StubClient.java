package usr.test;

import usr.net.*;
import usr.router.NetIF;
import usr.router.TCPNetIF;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubClient {
    final static int PORT_NUMBER = 4433;
    ConnectionOverTCP connection;
    NetIF netIF;

    public StubClient(String host, int port) {
        try {
	    // initialise socket
            TCPEndPointSrc src = new TCPEndPointSrc(host, port);
            //connection = new ConnectionOverTCP(src);
            //connection.setAddress(new IPV4Address("localhost"));
            //connection.connect();

            netIF = new TCPNetIF(src);
            netIF.connect();
            

            System.err.println("StubClient: Connected to: " + host);


        } catch (UnknownHostException uhe) {
            System.err.println("StubClient: Unknown host " + host);
            System.exit(1);
        } catch (IOException ioexc) {
            System.err.println("StubClient: Cannot connect to " + host + "on port " + port);
            System.exit(1);
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
            //// ORIG datagram = new IPV4Datagram(buffer); 
            datagram = DatagramFactory.newDatagram(Protocol.DATA, buffer);


            datagram.setDstAddress(new GIDAddress(47));

            if (netIF.sendDatagram(datagram) == false) {
                return;
            } else {

                //System.out.println("Sent: " + datagram + " with " + new String(datagram.getPayload()));
            }
        }

        netIF.close();
    }


    public static void main(String[] args) throws IOException {
        int count = 100;
        int port = PORT_NUMBER;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
        }

        if (args.length == 2) {
            // get no of writes
            Scanner scanner = new Scanner(args[1]);

            port = scanner.nextInt();
        }

        StubClient client = new StubClient("localhost",  port);
        client.writeALot(count);
    }


}

