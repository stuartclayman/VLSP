package usr.net;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import usr.logging.Logger;
import usr.logging.USR;

// Can't import them, as both used in this class
//import java.nio.channels.ClosedByInterruptException;
//import usr.net.ClosedByInterruptException;


/**
 * Create a connection that sends data
 * as USR Datagrams over TCP.
 */
public class ConnectionOverTCP implements Connection {
    // End point
    TCPEndPoint endPoint;

    static final int PACKETS_BEFORE_SHUFFLE = 10;
    static final byte [] checkbytes = "USRD".getBytes();
    // The underlying connection
    SocketChannel channel;

    // The local address for this connection
    Address localAddress;

    // A ByteBuffer to read into
    int bufferSize_ = PACKETS_BEFORE_SHUFFLE * 2  * 2048;
    ByteBuffer buffer;

    // current position in the ByteBuffer
    int bufferEndData_ = 0;
    int bufferStartData_ = 0;

    // counts
    int inCounter = 0;
    int outCounter = 0;

    boolean socketClosing_ = false;

    // eof
    boolean eof = false;


    /**
     * Construct a ConnectionOverTCP given a TCPEndPointSrc
     */
    public ConnectionOverTCP(TCPEndPointSrc src) throws IOException {
        endPoint = src;
        buffer = ByteBuffer.allocate(bufferSize_);
    }

    /**
     * Construct a ConnectionOverTCP given a TCPEndPointDst
     */
    public ConnectionOverTCP(TCPEndPointDst dst) throws IOException {
        endPoint = dst;
        buffer = ByteBuffer.allocate(bufferSize_);
    }

    /**
     * Connect.
     */
    @Override
	public boolean connect() throws IOException {
        endPoint.connect();

        Socket socket = endPoint.getSocket();

        if (socket == null) {
            throw new Error("EndPoint: " + endPoint + " is not connected");
        }


        channel = socket.getChannel();
        int i;
        int MAX_TRIES = 25;

        for (i = 0; i < MAX_TRIES; i++) {
            if (channel.finishConnect()) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

        }

        if (i == MAX_TRIES) {
            Logger.getLogger("log").logln(USR.ERROR, "Could not connect");
            return false;
        }

        if (channel == null) {
            throw new Error("Socket: " + socket + " has no channel");
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

    /** Send datagram down channel -- must be synchronized to prevent close occuring
     * when this is working
     */
    @Override
	public synchronized boolean sendDatagram(Datagram dg) throws IOException, usr.net.ClosedByInterruptException {
        //Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverTCP: send(" + outCounter + ")");
        if (dg == null) {
            Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverTCP: received null datagram");
            return false;
        }

        if (channel.isOpen()) {
            try {
                boolean success = writeBytesToChannel(((DatagramPatch)dg).toByteBuffer());

                if (success == false) {
                    throw new IOException ("Channel closed to write");
                }
                outCounter++;
                return true;
            } catch (java.nio.channels.ClosedByInterruptException cbie) {
                //Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverTCP: write failed because ClosedByInterruptException");
                throw new usr.net.ClosedByInterruptException("Connection from " + getAddress() + " Closed by Interrupt");
            }
        } else {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          "ConnectionOverTCP: " + endPoint + " outCounter = " + outCounter +
                                          " ALREADY CLOSED -- channel is closed");

            return false;
        }

    }

    public boolean writeBytesToChannel(ByteBuffer bb) throws IOException {

        int len = channel.write(bb);

        if (len < 0) {
            return false;
        }

        while (bb.remaining()>0) {

            len = channel.write(bb);

            if (len < 0) {
                return false;
            }

        }
        return true;
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
            throw new IOException("ConnectionOverTCP: Null in readDatagram()");
        }
    }

    /** look at buffer and try to decode a datagram from it without reading more data */
    Datagram decodeDatagram() {
        while (true) {

            // if we hit EOF an anytime, return null
            if (eof) {
                return null;
            }

            // Read a datagram at pos bufferStartData_ -- it may be partly read into buffer
            // and it may have other datagrams following it in buffer

            // Do we have enough space left in buffer to read at least
            // packet length if not then shuffle the buffer and try again.
            if (bufferSize_ - bufferStartData_ < 8) {
                shuffleBuffer();
                continue;
            }

            // Do we have enough data to read packetLen -- if not return null
            if (bufferEndData_ - bufferStartData_ < 8) {
                readMoreData();
                continue;
            }

            short packetLen = getPacketLen();

            // Because of buffer position we cannot read a full packet
            // -- shuffle the buffer and retry
            if (packetLen > bufferSize_ - bufferStartData_) {

                shuffleBuffer();
                readMoreData();

                continue;
            }

            // Not enough data has been read to get a packet
            if (bufferEndData_ - bufferStartData_ < packetLen) {
                readMoreData();

                continue;
            }

            // If our buffer is too short (want several packets before recopy)
            //, make it longer and read more data

            if (packetLen * PACKETS_BEFORE_SHUFFLE > bufferSize_) {
                Logger.getLogger("log").logln(USR.STDOUT, "Connection over TCP: Increasing buffer size");
                bufferSize_ = packetLen * PACKETS_BEFORE_SHUFFLE *2;
                ByteBuffer bigB = ByteBuffer.allocate(bufferSize_);
                int bufferRead = bufferEndData_- bufferStartData_;
                buffer.position(bufferStartData_);
                buffer.limit(bufferEndData_);
                bigB.put(buffer);
                buffer = bigB;
                bufferStartData_ = 0;
                bufferEndData_ = bufferRead;
                // SC return decodeDatagram();
                continue;
            }

            // OK -- we got a full packet of data, let's make a datagram of it
            if (bufferEndData_ - bufferStartData_ < packetLen) {
                throw new Error("ConnectionOverTCP: Bug in decodeDatagram()");
            }

            byte[] latestDGData = new byte[packetLen];

            //Logger.getLogger("log").logln(USR.STDOUT, "READING PACKET FROM "+bufferStartData_+ " to "+
            //  (bufferStartData_+packetLen));
            buffer.position(bufferStartData_);
            buffer.get(latestDGData);
            //for (int i= 0; i < packetLen; i++) {
            //   Logger.getLogger("log").logln(USR.ERROR, "At pos"+i+" char is "+ (char) latestDGData[i]);
            //}

            bufferStartData_ += packetLen;
            ByteBuffer newBB = ByteBuffer.wrap(latestDGData);
            // get an empty Datagram
            Datagram dg = DatagramFactory.newDatagram();
            // and fill in contents
            // not just the payload, but all headers too
            ((DatagramPatch)dg).fromByteBuffer(newBB);

            checkDatagram(latestDGData, dg);
            return dg;

        }
    }

    /** Read more data from channel to buffer if possible */
    void readMoreData() {
        buffer.position(bufferEndData_);

        //Logger.getLogger("log").logln(USR.ERROR, "ConnectionOverTCP: readMoreData: buffer position = " + bufferStartData_ + "/" +
        // bufferEndData_ + "/" + buffer.limit() + "/" + buffer.capacity());

        try {
            int count = channel.read(buffer);

            if (count == -1) {
                eof = true;
                return;
            } else {
                bufferEndData_ += count;
            }

        } catch (IOException ioe) {
            eof = true;  // Error occurs on shut down sometimes as pipe must close from one end
            //  Logger.getLogger("log").logln(USR.ERROR, "Connection over TCP read error "+ioe.getMessage());
            return;
        }
    }

    void checkDatagram (byte [] latestDGData, Datagram dg) {
        if (latestDGData[0] != checkbytes[0] ||
            latestDGData[1] != checkbytes[1] ||
            latestDGData[2] != checkbytes[2] ||
            latestDGData[3] != checkbytes[3]) {
            Logger.getLogger("log").logln(USR.ERROR, "Read incorrect datagram "+ java.util.Arrays.toString(latestDGData));
            Logger.getLogger("log").logln(USR.ERROR, "Buffer size "+bufferSize_+" start pos "+bufferStartData_ +
                                          " end Pos "+bufferEndData_);
            ByteBuffer b = ((DatagramPatch)dg).toByteBuffer();
            Logger.getLogger("log").logln(USR.ERROR, "READ as bytes "+ b.asCharBuffer());
            System.exit(-1);
        }
    }

    void shuffleBuffer() {
        //Logger.getLogger("log").logln(USR.ERROR, "Shuffling the buffer " + inCounter);
        int remaining = bufferEndData_-bufferStartData_;

        if (remaining == 0) {
            bufferStartData_ = 0;
            bufferEndData_ = 0;
            buffer.position(0);
            return;
        }
        // this is a single copy shuffle
        ByteBuffer newBuf = ByteBuffer.allocate(bufferSize_);
        buffer.position(bufferStartData_);
        newBuf.put(buffer);
        buffer = newBuf;

        bufferEndData_ = remaining;
        bufferStartData_ = 0;
    }

    /** Get length of packet from data in buffer -- implicit assumption here
       about position of data*/
    short getPacketLen() {
        short pktLen = buffer.getShort(bufferStartData_+5);
        //Logger.getLogger("log").logln(USR.ERROR, "READ PACKET LENGTH "+pktLen);
        return pktLen;
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

        Socket socket = getSocket();
        try {
            eof = true;
            socket.close();
        } catch (IOException ioe) {
            throw new Error("Socket: " + socket + " can't close");
        }

        Logger.getLogger("log").logln(USR.STDOUT, "ConnectionOverTCP: closed inCounter = " + inCounter + " outCounter = " + outCounter);

    }

    /**
     * Get the EndPoint of this Connection.
     */
    @Override
	public EndPoint getEndPoint() {
        return endPoint;
    }

    /**
     * Get the socket.
     */
    public Socket getSocket() {
        return endPoint.getSocket();
    }

    /**
     * Get the channel.
     */
    @SuppressWarnings("unused")
	private SocketChannel getChannel() {
        return endPoint.getSocket().getChannel();
    }

    /**
     * To String
     */
    @Override
	public String toString() {
        return endPoint.toString() + " " + getSocket().toString();
    }

}
