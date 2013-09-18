package demo_usr.paths;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.DatagramFactory;

/**
 * This class reads data from a UDP socket, converts the UDP packet
 * to a USR datagram and then sends it to a queue.
 */
public class UDPReader implements Callable <Object>{
    java.net.DatagramSocket inSocket;
    int udpPort;
    LinkedBlockingDeque<usr.net.Datagram> queue;
    boolean running = false;

    int count = 0;

    // verbose
    int verbose = 0;

    // Latch for end synchronization
    private final CountDownLatch actuallyFinishedLatch;


    public UDPReader(int udpPort, int recvBufSize, LinkedBlockingDeque<usr.net.Datagram> queue, int verbose) throws Exception {
        // the socket to read from
        // set up inbound UDP socket
        //inSocket = new java.net.DatagramSocket(udpPort);

        DatagramChannel channel = DatagramChannel.open();
        inSocket = channel.socket();
        inSocket.bind(new InetSocketAddress(udpPort));

        inSocket.setReceiveBufferSize(recvBufSize * 1024);

        // not like this for java.net.DatagramSocket
        // inSocket.bind(new InetSocketAddress(udpPort));

        // The queue to put new DatagramPackets on
        this.queue = queue;

        // the UDP port to listen to
        this.udpPort = udpPort;

        // verbose
        this.verbose = verbose;

        actuallyFinishedLatch = new CountDownLatch(1);

        running = true;
    }


    @Override
	public Object call() {
        // allocate a DatagramPacket
        java.net.DatagramPacket inDatagram = null;

        try {
            if (verbose > 0) {
                Logger.getLogger("log").logln(USR.STDOUT, "UDPReader: ReceiveBufferSize: " +inSocket.getReceiveBufferSize());
            }



            while (running) {
                // new space for each packet
                byte[] udpBuffer = new byte[2048];
                inDatagram = new java.net.DatagramPacket(udpBuffer, 2048);

                // read a UDP packet
                try {
                    inSocket.receive(inDatagram);
                } catch (java.nio.channels.ClosedByInterruptException cbie) {
                    //Logger.getLogger("log").logln(USR.ERROR, "UDPReader interrupted");
                    running = false;
                    break;
                }


                count++;



                /*
                 * convert DatagramPacket to Datagram
                 */
                // original data
                byte [] data = new byte[inDatagram.getLength()];
                System.arraycopy(inDatagram.getData(), 0, data, 0, inDatagram.getLength());

                // copy

                // now create a USR Datagram
                Datagram queueDatagram = DatagramFactory.newDatagram(data);



                queue.add(queueDatagram);

                //System.err.println("ReadThread IN  " + count + " recv: " +  inDatagram.getLength());
                /*
                  if (count % 100 == 0) {

                  System.err.println("ReadThread " + count + " queue size: " + queue.size());
                  }
                */
            }



        } catch (SocketException se) {
            Logger.getLogger("log").log(USR.ERROR, "UDPReader: SocketException: " + se.getMessage());

        } catch (IOException ioe) {
            Logger.getLogger("log").log(USR.ERROR, "UDPReader: " + ioe.getClass().getName() + ": " + ioe.getMessage());

        } catch (Throwable t) {
            Logger.getLogger("log").log(USR.ERROR, "UDPReader: " + t.getMessage());
            t.printStackTrace();

        } finally {

            if (inSocket != null) {
                inSocket.close();
            }

        }


        actuallyFinishedLatch.countDown();


        return null;

    }

    /**
     * Wait for this Reader to terminate.
     */
    public void await() {
        try {
            actuallyFinishedLatch.await();
        } catch (InterruptedException ie) {
        }
    }


}

