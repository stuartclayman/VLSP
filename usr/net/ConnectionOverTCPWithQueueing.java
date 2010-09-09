package usr.net;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Create a network connection that sends data 
 * as USR Datagrams.
 * This queues up all Datagrams that get received on rach read.
 */
public class ConnectionOverTCPWithQueueing extends ConnectionOverTCP {
    // a Queue of incoming Datagrams
    Queue<Datagram> queue;

    /**
     * Construct a DatagramConnection given a TCPEndPointSrc.
     */
    public ConnectionOverTCPWithQueueing(TCPEndPointSrc src) throws IOException {
        super(src);
        queue = new LinkedList<Datagram>();    
    }

    /**
     * Construct a DatagramConnection given a TCPEndPointDst.
     */
    public ConnectionOverTCPWithQueueing(TCPEndPointDst dst) throws IOException{
        super(dst);
        queue = new LinkedList<Datagram>();    
    }

    /**
     * Read a Datagram.
     */
    public Datagram readDatagram() {
        if (queue.size() == 0) {
            // nothing in queue so read from channel

            try {
                // System.err.println("DatagramConnection readDatagram: pre-read buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());
                int startPosition = buffer.position();
                int count = channel.read(buffer);

                // System.err.println("DatagramConnection: read " + count);

                if (count == -1) {
                    // reached EOF
                    return null;
                } else {
                    // sort out ByteBuffer
                    int bufferLimit = count + startPosition;
                    buffer.clear();
                    buffer.limit(bufferLimit);

                    // now find Datagrams
                    current = 0;

                    while (buffer.position() < buffer.limit()) {

                        // System.err.println("DatagramConnection readDatagram: buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());

                        short totalLen;

                        // check if there is enough to find the message total len
                        if (buffer.limit() - buffer.position() > 8) {
                            buffer.position(current + 5);
                            totalLen = buffer.getShort();
                        } else {
                            totalLen = Short.MAX_VALUE;
                        }


                        if (totalLen + current > BUF_SIZE) {
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

                            // System.err.println("DatagramConnection readDatagram: post-shuffle buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());
                            break;

                        } else {

                            buffer.position(totalLen + current);
                            buffer.mark();
                            buffer.position(current);

                            ByteBuffer newBB = buffer.slice();
                            Datagram dg = new IPV4Datagram();                        
                            ((DatagramPatch)dg).fromByteBuffer(newBB);

                            // add to queue
                            queue.add(dg);

                            // skip to next
                            current += totalLen;
                            buffer.position(current);
                        }
                    }

                    // check if ended with a shuffle
                    // System.err.println("DatagramConnection readDatagram: post-while buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());
                    
                    //  at end, so queued everything
                    if (buffer.position() == buffer.limit()) {
                        buffer.clear();
                        current = 0;
                        // System.err.println("DatagramConnection readDatagram: post-while buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());
                    }



                    // return a Datagram
                    return queue.poll();
                }
            } catch (IOException ioe) {
                return null;
            }
        } else {
            // return head of queue
            return queue.poll();
        }
    }


}
