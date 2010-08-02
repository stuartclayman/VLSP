package usr.test;

import usr.net.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubClient {
    final static int PORT_NUMBER = 4433;
    DatagramConnection connection;

    public StubClient(String host, int port) {
        Socket socket = null;

        try {
	    // initialise socket
            SocketChannel channel = SocketChannel.open();
            socket = channel.socket();
            socket.connect(new InetSocketAddress(host, port));
            // WAS socket = new Socket(InetAddress.getByName(host), port);

            System.err.println("Connected to: " + host);

            connection = new DatagramConnection(socket);
            connection.setAddress(new IPV4Address("localhost"));

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
        Datagram datagram;

        for (int i = 0; i < count; i++) {
            String line = "line " + i;
            ByteBuffer buffer = ByteBuffer.allocate(line.length());
            buffer.put(line.getBytes());
            datagram = new IPV4Datagram(buffer);

            String addr = "192.168.7.1";
            try {
                datagram.setDstAddress(new IPV4Address(addr));
            } catch (UnknownHostException uhe) {
                System.err.println("UnknownHostException " + addr);
            }

            if (connection.sendDatagram(datagram) == false) {
                return;
            } else {

                /*
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
                */

                System.out.println("Sent: " + datagram + " with " + new String(datagram.getPayload()));
            }
        }
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

