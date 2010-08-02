package usr.test;

import usr.net.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubServer {
    final static int PORT_NUMBER = 4433;
    DatagramConnection connection;
    ServerSocket serverSocket;

    public StubServer(int listenPort) throws IOException {
	// initialise the socket
        try {
            ServerSocketChannel channel = ServerSocketChannel.open();
            serverSocket = channel.socket();
            serverSocket.bind(new InetSocketAddress(listenPort));
            // WAS serverSocket = new ServerSocket(listenPort);

            System.err.println("StubServer: Listening on port: " + PORT_NUMBER);
        } catch (IOException ioe) {
            System.err.println("StubServer: Cannot listen on port: " + PORT_NUMBER);
            throw ioe;
        }
    }

    /**
     * Accept a connection.
     */
    void accept() throws IOException {
	// wait for the receiver and server to connect (order unimportant)
	Socket receiverSocket = serverSocket.accept();
	System.out.println("StubServer: Received connection from: " +
				receiverSocket.getInetAddress().getHostName());


        connection = new DatagramConnection(receiverSocket);

    }

    /**
     * Read stuff
     */
    void readALot() {
        Datagram datagram;
        int count = 0;

        long t0 = System.currentTimeMillis();

        while ((datagram = connection.readDatagram()) != null) {
            System.out.print(count + ". ");
            System.out.print("HL: " + datagram.getHeaderLength() +
                             " TL: " + datagram.getTotalLength() +
                             " From: " + datagram.getSrcAddress() +
                             " To: " + datagram.getDstAddress() +
                             ". ");
            byte[] payload = datagram.getPayload();

            if (payload == null) {
                System.out.println("No payload");
            } else {
                System.out.println(new String(payload));
            }

            count++;
        }

        long t1 = System.currentTimeMillis();

        long elapsed = t1 - t0;
        int secs = (int) elapsed / 1000;
        int millis = (int) elapsed % 1000;

        NumberFormat millisFormat = new DecimalFormat("000"); 
        System.err.println("elapsed[" + count + "] = " + secs + ":" + millisFormat.format(millis));

    }

    public static void main(String[] args) throws IOException {
        StubServer server = new StubServer(PORT_NUMBER);

        while (true) {
            server.accept();
            server.readALot();
        }
    }
}
