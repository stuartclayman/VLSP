// USRReceiver.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2009

package plugins_usr.monitoring.distribution;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import usr.net.Address;
import usr.net.Datagram;
import usr.net.DatagramSocket;
import usr.net.SocketAddress;
import eu.reservoir.monitoring.core.TypeException;
import eu.reservoir.monitoring.distribution.Receiving;

/**
 * This is a USR receiver for monitoring messages.
 */
public class USRReceiver implements Runnable
{
/*
 * The receiver that interactes messages.
 */
Receiving receiver = null;

/*
 * The socket doing the listening
 */
DatagramSocket socket;

/*
 * A Datagram to receive
 */
Datagram packet;

/*
 * The address
 */
SocketAddress address;

Address dstAddr;

/*
 * The port
 */
int port;

/*
 * My thread.
 */
Thread myThread;

boolean threadRunning = false;

/*
 * A default packet size.
 */
static int PACKET_SIZE = 65535;      // was 1500;

/*
 * The packet contents as a ByteArrayInputStream
 */
ByteArrayInputStream byteStream;

/*
 * The Address of the last packet received
 */
Address srcAddr;

/*
 * The length of the last packet received
 */
int length;

/*
 * The last exception received.
 */
Exception lastException;

/**
 * Construct a receiver for a particular address
 */
public USRReceiver(Receiving receiver,
    SocketAddress ipAddr) throws IOException {
    address = ipAddr;

    this.receiver = receiver;
    this.dstAddr = ipAddr.getAddress();
    this.port = ipAddr.getPort();

    setUpSocket();
}

/**
 * Set up the socket for the given addr/port,
 * and also a pre-prepared DatagramPacket.
 */
void setUpSocket() throws IOException {
    socket = new DatagramSocket(port);
}

/**
 * Join the address now
 * and start listening
 */
public void listen()  throws IOException {
    // already bind to the address
    //socket.bind(address);

    // start the thread
    myThread = new Thread(this);

    myThread.start();
}

/**
 * Leave the address now
 * and stop listening
 */
public void end()  throws IOException {
    // stop the thread
    threadRunning = false;

    // disconnect
    socket.disconnect();
}

/**
 * Receive a  message from the multicast address.
 */
protected boolean receive(){
    try {
        // clear lastException
        lastException = null;

        // receive from socket
        packet = socket.receive();

        if (packet == null) {
            // we hit EOF

            return false;
        } else {
            /* System.out.println("USRReceiver Received " +
             * packet.getLength() +
             * " bytes from "+ packet.getAddress() +
             * "/" + packet.getPort());
             */

            // get an input stream over the data bytes of the packet
            byte [] payload = packet.getPayload();
            ByteArrayInputStream theBytes =
                new ByteArrayInputStream(
                    payload, 0, payload.length);

            byteStream = theBytes;
            srcAddr = packet.getSrcAddress();
            length = packet.getTotalLength();

            return true;
        }
    } catch (Exception e) {
        // something went wrong
        lastException = e;
        return false;
    }
}

/**
 * The Runnable body
 */
@Override
public void run(){
    // if we get here the thread must be running
    threadRunning = true;

    while (threadRunning) {
        if (receive()) {
            // construct the transmission meta data
            USRTransmissionMetaData metaData =
                new USRTransmissionMetaData(length, srcAddr,
                    dstAddr);

            // now notify the receiver with the message
            // and the address it came in on
            try {
                receiver.received(byteStream, metaData);
            } catch (IOException ioe) {
                receiver.error(ioe);
            } catch (TypeException te) {
                receiver.error(te);
            }
        } else {
            // the receive() failed
            // first, we try to find the exception in lastException
            // if it is null then we reached EOF
            if (lastException == null)
                receiver.eof();
            else
                receiver.error(lastException);
        }
    }
}
}