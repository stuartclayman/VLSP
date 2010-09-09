package usr.net;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Create a connection that sends data 
 * as USR Datagrams over TCP.
 */
public class ConnectionOverTCP implements Connection {
    // End point
    TCPEndPoint endPoint;

    // The underlying connection
    SocketChannel channel;

    // The local address for this connection
    Address localAddress;

    // A ByteBuffer to read into
    int BUF_SIZE = 2048;
    ByteBuffer buffer;

    // current position in the ByteBuffer
    int current = 0;

    /**
     * Construct a ConnectionOverTCP given a TCPEndPointSrc
     */
    public ConnectionOverTCP(TCPEndPointSrc src) throws IOException {
        endPoint = src;
        buffer = ByteBuffer.allocate(BUF_SIZE);
    }

    /**
     * Construct a ConnectionOverTCP given a TCPEndPointDst
     */
    public ConnectionOverTCP(TCPEndPointDst dst) throws IOException {
        endPoint = dst;
        buffer = ByteBuffer.allocate(BUF_SIZE);
    }

    /**
     * Connect.
     */
    public boolean connect() throws IOException {
        endPoint.connect();

        Socket socket = endPoint.getSocket(); 

        if (socket == null) {
            throw new Error("EndPoint: " + endPoint + " is not connected");
        }
        

        channel = socket.getChannel();

        if (channel == null) {
            throw new Error("Socket: " + socket + " has no channel");
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
        // set the source address and port on the Datagram
        dg.setSrcAddress(localAddress);

        try {
            int count = channel.write(((DatagramPatch)dg).toByteBuffer());

            if (count == -1) {
                return false;
            } else {
                // System.err.println("ConnectionOverTCP: write " + count);

                return true;
            }
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Read a Datagram.
     */
    public Datagram readDatagram() {
            try {
                int startPosition = buffer.position();
                int count = 0;
                short totalLen = 0;


                // empty buffer, so read
                if (buffer.position() == 0 ) {
                    // System.err.println("ConnectionOverTCP readDatagram: pre-read buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());
                    count = channel.read(buffer);

                    if (count == -1) {
                        // reached EOF
                        return null;
                    } else {
                        current = 0;
                        buffer.clear();
                        buffer.limit(count);                    
                    }
                }

                // System.err.println("ConnectionOverTCP readDatagram: buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());

                // check if there is enough to find the message total len
                boolean canCheckTotalLen = false;

                if (buffer.limit() - buffer.position() > 8) {
                    canCheckTotalLen = true;
                    buffer.position(current + 5);
                    totalLen = buffer.getShort();
                } else {
                    canCheckTotalLen = false;
                }

                // edge case, where we only have part of a message
                if (!canCheckTotalLen || (totalLen + current > BUF_SIZE)) {
                    // there is part of a message
                    // copy from current to end of ByteBuffer
                    // to the start of the buffer
                    int remaining = BUF_SIZE - current;
                    byte[] spare = new byte[remaining];

                    // reposition to start of message
                    buffer.position(current);
                    buffer.get(spare);

                    // reset ByteBuffer and copy back in
                    buffer.clear();
                    buffer.put(spare);
                    buffer.position(remaining);

                    // System.err.println("ConnectionOverTCP readDatagram: post-shuffle buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());
                    // need to read the next part of message
                    count = channel.read(buffer);


                    // now we have enough message to get the totalLen

                    if (count == -1) {
                        // reached EOF
                        return null;
                    } else {
                        current = 0;
                        buffer.position(0);
                        buffer.limit(count + remaining);

                        buffer.position(current + 5);
                        totalLen = buffer.getShort();

                    }

                }

                // check if buffer is big enough
                if (false) { // if (buffer.capacity() < totalLen) {
                    ByteBuffer biggerBuf = ByteBuffer.allocate(totalLen);
                    BUF_SIZE = totalLen;
                    // System.err.println("ConnectionOverTCP readDatagram: realloc buffer = " + biggerBuf.position() + " < " + biggerBuf.limit() + " < " + biggerBuf.capacity());

                    buffer.position(current); 
                    biggerBuf.put(buffer);

                    // System.err.println("ConnectionOverTCP readDatagram: post-copy buffer = " + biggerBuf.position() + " < " + biggerBuf.limit() + " < " + biggerBuf.capacity());
                    buffer = biggerBuf;

                    // need to read the next part of the message
                    // and fill up buffer
                    while (buffer.position() < buffer.capacity()) {
                        count = channel.read(buffer);

                        if (count == -1) {
                            // reached EOF
                            return null;
                        } 
                    }
                }



                /*
                  System.err.println("Datagram start = " + buffer.position());
                  System.err.println("Datagram current = " + current);
                  System.err.println("Datagram totalLen = " + totalLen);
                */

                // now get Datagram
                buffer.position(current);

                byte[] latestDGData = new byte[totalLen];
                buffer.get(latestDGData);
                Datagram dg = new IPV4Datagram();                        
                ByteBuffer newBB = ByteBuffer.wrap(latestDGData);
                ((DatagramPatch)dg).fromByteBuffer(newBB);

                // align for next message
                current += totalLen;
                buffer.position(current);

                // check if at end
                if (buffer.position() == buffer.limit()) {
                    // read last message
                    buffer.clear();
                }

                return dg;

            } catch (IOException ioe) {
                // TODO:  return Datagram.ERROR object
                return null;
            }
    }

    /**
     * Close the connection.
     */
    public void close() {
        // send a ControlMessage
        // TODO:  implement control messages
        // controlClose();

        Socket socket = getSocket();

        try {
            socket.close();
        } catch (IOException ioe) {
            throw new Error("Socket: " + socket + " can't close");
        }
    }

    /**
     * Get the EndPoint of this Connection.
     */
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
     * To String
     */
    public String toString() {
        return endPoint.toString() + " " + getSocket().toString();
    }


    /**
     * Consturct and send a control message.
     */
    protected boolean controlClose() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put("CLOSE".getBytes());
        Datagram datagram = new IPV4Datagram(buffer);
        datagram.setProtocol(1);

        return sendDatagram(datagram);

        
    }


}
