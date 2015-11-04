package usr.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import usr.logging.Logger;
import usr.logging.USR;

/**
 * Create a connection that sends data
 * as USR Datagrams over UDP.
 */
public class ConnectionOverUDP implements Connection {
    // End point
    UDPEndPoint endPoint;

    static final byte [] checkbytes = "USRD".getBytes();

    // The local address for this connection
    Address localAddress;

    // A ByteBuffer to read into
    int BUF_SIZE = 2048;
    int bufferSize_ = BUF_SIZE;
    ByteBuffer buffer;

    // reveice array
    byte[] recvArray;
    DatagramPacket recvPacket;

    int current = 0;
    boolean socketClosing_ = false;

    // counts
    int inCounter = 0;
    int outCounter = 0;

    // eof
    boolean eof = false;


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
    @Override
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
    @Override
    public Address getAddress() {
        return localAddress;
    }

    /**
     * Set the Address for this connection.
     */
    @Override
    public Connection setAddress(Address addr) {
        localAddress = addr;
        return this;
    }

    /**
     * Send a Datagram.
     */
    @Override
    public synchronized boolean sendDatagram(Datagram dg) throws IOException {
        if (dg == null) {
            Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverUDP: received null datagram");
            return false;
        }

        DatagramSocket socket = getSocket();

        if (! socket.isClosed()) {

            // convert byte buffer to a DatagramPacket
            byte[] data = ((DatagramPatch)dg).toByteBuffer().array();

            // UDP packet
            DatagramPacket packet = new DatagramPacket(data, data.length);

            /*
            // set destination address
            // These are in the endPoint
            if (endPoint instanceof UDPEndPointSrc) {
                // this is good
                UDPEndPointSrc src = (UDPEndPointSrc)endPoint;

                InetAddress addr = src.getRemoteHost();
                packet.setAddress(addr);
                packet.setPort(src.getRemotePort());
            } else if (endPoint instanceof UDPEndPointDst) {
                // this is good
                UDPEndPointDst dst = (UDPEndPointDst)endPoint;

                InetAddress addr = dst.getRemoteHost();
                packet.setAddress(addr);
                packet.setPort(dst.getRemotePort());
            } else {
                throw new Error("ConnectionOverUDP: cannot send from a NON EndPoint destination");
            }
            */

            //Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverUDP sendDatagram: packet = " + packet);
            try {

                // send it
                socket.send(packet);
                outCounter++;

                return true;
            } catch (java.net.PortUnreachableException pue) {
                DatagramSocket endPointSocket = endPoint.getSocket();
                Logger.getLogger("log").logln(USR.STDOUT, pue.getMessage() + " to " + endPointSocket.getLocalAddress() + endPointSocket.getLocalPort() + " <-> " + endPointSocket.getInetAddress() + endPointSocket.getPort());
                return false;
            }

        } else {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          "ConnectionOverUDP: " + endPoint + " outCounter = " + outCounter +
                                          " ALREADY CLOSED -- channel is closed");

            return false;
        }
    }

    /**
     * Read a Datagram.
     */
    @Override
    public Datagram readDatagram() throws IOException {
        Datagram dg;

        if (eof) {
            // hit eof, so really return null
            return null;
        }

        dg = decodeDatagram();

        if (dg != null) {
            inCounter++;
            return dg;
        } else {
            throw new IOException("ConnectionOverUDP: Unexpected Null in readDatagram()");
        }
    }

    /** look at buffer and try to decode a datagram from it without reading more data */
    Datagram decodeDatagram() {
        // if we hit EOF an anytime, return null
        if (eof) {
            return null;
        }

        DatagramSocket socket = getSocket();

        try {
            buffer.position();
            int count = 0;
            short totalLen = 0;


            // read
            socket.receive(recvPacket);
            count = recvPacket.getLength();

            current = 0;
            buffer.clear();
            buffer.limit(count);

            //Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverUDP readDatagram: buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());

            totalLen = (short)count;

            // now get Datagram
            byte[] latestDGData = new byte[totalLen];
            buffer.get(latestDGData);
            ByteBuffer newBB = ByteBuffer.wrap(latestDGData);
            // get an empty Datagram
            Datagram dg = DatagramFactory.newDatagram();
            // and fill in contents
            // not just the payload, but all headers too
            ((DatagramPatch)dg).fromByteBuffer(newBB);

            //buffer.clear();

            checkDatagram(latestDGData, dg);
            return dg;

        } catch (IOException ioe) {
            // TODO:  return Datagram.ERROR object
            eof = true;

            //Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverUDP Exception in readDatagram: " + ioe);
            //ioe.printStackTrace();


            return null;
        }
    }

    void checkDatagram (byte [] latestDGData, Datagram dg) {
        if (latestDGData[0] != checkbytes[0] ||
            latestDGData[1] != checkbytes[1] ||
            latestDGData[2] != checkbytes[2] ||
            latestDGData[3] != checkbytes[3]) {
            Logger.getLogger("log").logln(USR.ERROR, "Read incorrect datagram "+ java.util.Arrays.toString(latestDGData));
            //Logger.getLogger("log").logln(USR.ERROR, "Buffer size "+bufferSize_+" start pos "+bufferStartData_ + " end Pos "+bufferEndData_);
            ByteBuffer b = ((DatagramPatch)dg).toByteBuffer();
            Logger.getLogger("log").logln(USR.ERROR, "READ as bytes "+ b.asCharBuffer());
            System.exit(-1);
        }
    }

    /**
     * Close the connection -- must be synchronized to prevent close while
     * we are in sendDatagram
     */
    public synchronized void close() {
        if (socketClosing_) {
            return;
        }
        socketClosing_ = true;

        DatagramSocket socket = getSocket();
        try {
            eof = true;
            socket.close();
        } catch (Exception ioe) {
            throw new Error("Socket: " + socket + " can't close");
        }


        Logger.getLogger("log").logln(USR.STDOUT, "ConnectionOverUDP: closed inCounter = " + inCounter + " outCounter = " + outCounter);
    }


    /**
     * Get the EndPoint of this Connection.
     */
    @Override
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
    @Override
    public String toString() {
        return endPoint.toString();
    }

}
