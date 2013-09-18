// USRTransmitter.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.distribution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import usr.net.Address;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;
import usr.net.SocketAddress;
import eu.reservoir.monitoring.distribution.Transmitting;

/**
 * This is a USR transmitter for monitoring messages
 */
public class USRTransmitter
{
/*
 * The transmitting that interacts with a DataSourceDelegate.
 */
Transmitting transmitting = null;

/*
 * The socket being transmitted to
 */
DatagramSocket socket;

/*
 * SocketAddress
 */
SocketAddress dstAddr;

/*
 * A Datagram being transmitted
 */
Datagram packet;

/*
 * The Address
 */
Address address;

/*
 * The port
 */
int port;

/**
 * Construct a transmitter for a particular GID SocketAddress
 */
public USRTransmitter(Transmitting transmitting,
    SocketAddress dstAddr) throws IOException {
    this.dstAddr = dstAddr;

    this.transmitting = transmitting;
    this.address = dstAddr.getAddress();
    this.port = dstAddr.getPort();

    setUpSocket();
}

/**
 * Set up the socket for the given addr/port,
 * and also a pre-prepared Datagrapacket.
 */
void setUpSocket() throws IOException {
    socket = new DatagramSocket();
}

/**
 * Connect to the remote address now
 */
public void connect()  throws IOException {
    // connect to the remote USR socket
    socket.connect(dstAddr);
}

/**
 * End the connection to the remote address now
 */
public void end()  throws IOException {
    // disconnect now
    socket.disconnect();
}

/**
 * Send a message to USR address,  with a given id.
 */
public int transmit(ByteArrayOutputStream byteStream,
    int id) throws IOException {
    // set up the Datagram
    byte[] payload = byteStream.toByteArray();

    ByteBuffer buffer = ByteBuffer.wrap(payload);

    packet = DatagramFactory.newDatagram(buffer);

    // now send it
    socket.send(packet);

    //System.err.println("trans: " + id + " = " +
    // byteStream.size());

    // notify the transmitting object
    if (transmitting != null)
        transmitting.transmitted(id);

    return byteStream.size();
}
}