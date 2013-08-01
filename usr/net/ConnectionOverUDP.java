package usr.net;

import java.net.DatagramSocket;
import usr.logging.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Create a connection that sends data
 * as USR Datagrams over UDP.
 */
public class ConnectionOverUDP implements Connection {
    // End point
    UDPEndPoint endPoint;

    // The local address for this connection
    Address localAddress;

    // A ByteBuffer to read into
    int BUF_SIZE = 2048;
    ByteBuffer buffer;

    // reveice array
    byte[] recvArray;
    DatagramPacket recvPacket;

    // current position in the ByteBuffer
    int current = 0;

    /**
     * Construct a ConnectionOverUDP given a UDPEndPointSrc
     */
    public ConnectionOverUDP(UDPEndPointSrc src) throws IOException {
        endPoint = src;
        recvArray = new byte[BUF_SIZE];
        recvPacket = new DatagramPacket(recvArray, recvArray.length);
        buffer = ByteBuffer.wrap(recvArray);
    }

    /**
     * Construct a ConnectionOverUDP given a UDPEndPointDst
     */
    public ConnectionOverUDP(UDPEndPointDst dst) throws IOException {
        endPoint = dst;
        recvArray = new byte[BUF_SIZE];
        recvPacket = new DatagramPacket(recvArray, recvArray.length);
        buffer = ByteBuffer.wrap(recvArray);
    }

    /**
     * Connect.
     */
    public boolean connect() throws IOException {
        endPoint.connect();

        DatagramSocket socket = endPoint.getSocket();

        if (socket == null) {
            throw new Error("EndPoint: " + endPoint + " is not connected");
        }

        return true;
    }

    /**
     * Get the Address for this connection.
     */
    public Address getAddress() {
        return localAddress;
    }

    /**
     * Set the Address for this connection.
     */
    public Connection setAddress(Address addr) {
        localAddress = addr;
        return this;
    }

    /**
     * Send a Datagram.
     */
    public boolean sendDatagram(Datagram dg) {
        DatagramSocket socket = getSocket();

        // set the address
        dg.setSrcAddress(localAddress);

        try {
            // convert byte buffer to a DatagramPacket
            byte[] data = ((DatagramPatch)dg).toByteBuffer().array();
            DatagramPacket packet = new DatagramPacket(data, data.length);

            // set destination address
            // These are in the endPoint
            if (endPoint instanceof UDPEndPointSrc) {
                // this is good
                UDPEndPointSrc src = (UDPEndPointSrc)endPoint;

                InetAddress addr = InetAddress.getByName(src.getHostName());
                packet.setAddress(addr);
                packet.setPort(src.getPort());
            } else {
                throw new Error("ConnectionOverUDP: cannot send from an EndPoitn destination");
            }

            //Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverUDP sendDatagram: packet = " + packet);

            // send it
            socket.send(packet);

            return true;

        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Read a Datagram.
     */
    public Datagram readDatagram() {
        DatagramSocket socket = getSocket();

        try {
            int startPosition = buffer.position();
            int count = 0;
            short totalLen = 0;


            // read
            socket.receive(recvPacket);
            count = recvPacket.getLength();

            current = 0;
            buffer.clear();
            buffer.limit(count);

            // Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverUDP readDatagram: buffer = " + buffer.position() + " < " +
            // buffer.limit() + " < " + buffer.capacity());

            totalLen = (short)count;

            // now get Datagram
            byte[] latestDGData = new byte[totalLen];
            buffer.get(latestDGData);
            ByteBuffer newBB = ByteBuffer.wrap(latestDGData);

            Datagram dg = DatagramFactory.newDatagram();
            ((DatagramPatch)dg).fromByteBuffer(newBB);

            buffer.clear();

            return dg;

        } catch (IOException ioe) {
            // TODO:  return Datagram.ERROR object
            return null;
        }
    }

    /**
     * Get the EndPoint of this Connection.
     */
    public EndPoint getEndPoint() {
        return endPoint;
    }

    /**
     * Get the DatagramSocket.
     */
    public DatagramSocket getSocket() {
        return endPoint.getSocket();
    }

    /**
     * To String
     */
    public String toString() {
        return endPoint.toString();
    }

}