package demo_usr.paths;

import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.ClosedByInterruptException;
import usr.net.DatagramSocket;

/**
 * This class reads USR Datagrams from a USR DatagramSocket
 * and sends them to a queue
 */
public class USRReader implements Callable <Object>{
    usr.net.DatagramSocket inSocket;
    int usrPort;
    LinkedBlockingDeque<usr.net.Datagram> queue;
    boolean running = false;

    int count = 0;

    // verbose
    int verbose = 0;


    // Latch for end synchronization
    private final CountDownLatch actuallyFinishedLatch;


    public USRReader(int usrPort, LinkedBlockingDeque<usr.net.Datagram> queue, int verbose) throws Exception {
        // the socket to read from
        // set up inbound USR socket
        inSocket = new DatagramSocket();
        inSocket.bind(usrPort);

        // The queue to put new Datagrams on
        this.queue = queue;

        // the USR port to listen to
        this.usrPort = usrPort;

        // verbose
        this.verbose = verbose;

        actuallyFinishedLatch = new CountDownLatch(1);

        running = true;
    }


    @Override
	public Object call() {
        // allocate a Datagram
        usr.net.Datagram inDatagram = null;

        try {
            while (running) {
                // read a USR Datagram

                try {
                    inDatagram = inSocket.receive();
                } catch (ClosedByInterruptException cbie) {
                    //if (inDatagram == null) {
                    //Logger.getLogger("log").logln(USR.ERROR, "USRReader Interrupted");
                    running = false;
                    break;
                }


                queue.add(inDatagram);

                //System.err.println("ReadThread IN  " + count + " recv: " +  inDatagram.getLength());
                /*
                  if (count % 100 == 0) {

                  System.err.println("ReadThread " + count + " queue size: " + queue.size());
                  }
                */
            }

        } catch (SocketException se) {
            Logger.getLogger("log").log(USR.ERROR, "USRReader: SocketException: " + se.getMessage());

        } catch (Throwable t) {
            Logger.getLogger("log").log(USR.ERROR, "Throwable " + t.getClass() + ": " + t.getMessage());
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

