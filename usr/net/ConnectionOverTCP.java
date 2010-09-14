package usr.net;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Create a connection that sends data 
 * as USR Datagrams over TCP.
 * <p>
 * On the reading side, it queues up Datagrams that come
 * from the remote end.
 * <p>
 * It also implements control datagrams, so one end can inform the
 * other end of stuff.
 */
public class ConnectionOverTCP implements Connection, Runnable {
    // End point
    TCPEndPoint endPoint;

    // The underlying connection
    SocketChannel channel;

    // got a remote close 
    boolean remoteClose = false;

    // The local address for this connection
    Address localAddress;

    // A ByteBuffer to read into
    int BUF_SIZE = 2048;
    ByteBuffer buffer;

    // current position in the ByteBuffer
    int current = 0;

    // Read Thread
    Thread readThread = null;
    boolean paused = false;
    boolean reading = false;

    // Thread doing queue.take()
    Thread takeThread = null;
    boolean waitingForQueue = false;


    // Is the thread running
    boolean running = false;

    // a Queue of incoming Datagrams
    BlockingQueue<Datagram> queue;

    // queue high limit
    int QUEUE_PAUSE_LIMIT = 40;
    // queue low limit
    int QUEUE_TOO_LOW = 10;

    /**
     * Construct a ConnectionOverTCP given a TCPEndPointSrc
     */
    public ConnectionOverTCP(TCPEndPointSrc src) throws IOException {
        endPoint = src;
        buffer = ByteBuffer.allocate(BUF_SIZE);
        queue = new LinkedBlockingQueue<Datagram>();    
    }

    /**
     * Construct a ConnectionOverTCP given a TCPEndPointDst
     */
    public ConnectionOverTCP(TCPEndPointDst dst) throws IOException {
        endPoint = dst;
        buffer = ByteBuffer.allocate(BUF_SIZE);
        queue = new LinkedBlockingQueue<Datagram>();    
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
        
        start();

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
            int count = getChannel().write(((DatagramPatch)dg).toByteBuffer());

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
        // Get a Datagram from queue while queue has something in it
        // Only grab data if we are running or if there is residual 
        // stuff in the queue
        if ((running && queue.size() >= 0) || (!running && queue.size() > 0)) {
            // return a Datagram
            try {
                waitingForQueue = true;

                // which thread is doing the queue take()
                takeThread = Thread.currentThread();

                Datagram dg = queue.take();

                waitingForQueue = false;

                // if the reader is paused and
                // the queue is empty 
                // start reading again
                if (paused && queue.size() == QUEUE_TOO_LOW) {
                    readAgain();
                }


                return dg;
            } catch (InterruptedException ie) {
                //System.err.println("ConnectionOverTCP: readDatagram() interrupt");
                waitingForQueue = false;
                return null;
            }

        } else {
            //System.err.println("ConnectionOverTCP: readDatagram() return null");
            return null;
        }
    }

    /**
     * Read a Datagram.
     * This actually reads from the network connection.
     */
    Datagram readDatagramAndWait() {
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
        if (!remoteClose) {
            // if the close is initiated locally
            // send a control message to the other end
            controlClose();
            /*} else {
              System.err.println("ConnectionOverTCP: got remote close"); */
        }

        stop();

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
     * Get the channel.
     */
    private SocketChannel getChannel() {
        return endPoint.getSocket().getChannel();
    }

    /**
     * To String
     */
    public String toString() {
        return endPoint.toString() + " " + getSocket().toString();
    }

    /*
     * Methods to do with reading using a thread.
     */

    /**
     * Thread body to read from Connection.
     * This reads the Datagrams from the network
     * and queues them up ready to be collected.
     * If the queue is at the high limit then it waits until 
     * the queue has been drained to a certain level, the low limit,
     * and is told to start reading again.
     * <p>
     * However, if the Datagram is a control Datagram, then it is 
     * processed immediately and not put on the queue.
     */
    public void run() {
        //TODO: 
        // 1. implement more control packets
        // 2. implement state machine so NetIf can see the state of a Connection
        // 3. PAUSE a connection so it is connected but no traffic flows over it

        //System.err.println("ConnectionOverTCP: " + readThread + " top of run()");

	// sit in a loop and grab input
	while (running) {
            // if the queue has reached its limit
            // dont read any more until we get an interrupt
            if (queue.size() >= QUEUE_PAUSE_LIMIT) {

                //System.err.println("run() about to wait()");
                try {
                    synchronized (this) {
                        paused = true;
                        wait();
                    }
                } catch (InterruptedException ie) {
                    paused = false;
                    //System.err.println("run() out of wait()");
                }
            }

            // now go and read
            reading = true;

            Datagram datagram = readDatagramAndWait();

            reading = false;

            // check the return value
            if (datagram == null) {
                // EOF
                running = false;
            } else {
                if (datagram.getProtocol() == 1) {
                    // its a control datagram
                    processControlDatagram(datagram);
                } else {
                    // data
                    // add to queue
                    queue.add(datagram);
                }
            }
        }

        // the end
        theEnd();

    }

    /**
     * Notify main thread.
     */
    private void theEnd() {
        if (waitingForQueue) {
            System.err.println("ConnectionOverTCP:  interrupt " + takeThread);
            takeThread.interrupt();
        }
    }

    /**
     * Notify read thread.
     */
    private void readAgain() {
        // This causes the wait() in run() to be interrupted
        // and then real reading will start again
        readThread.interrupt();
    }
    
    /**
     * Start the thread.
     */
    public void start() {
        if (running == false) {
            readThread = new Thread(this, this.getClass().getName() + "-" + hashCode());
            running = true;
            readThread.start();
        }
    }

    /**
     * Stop the thread.
     */
    public void stop() {
        if (running == true) {
            try {
                running = false;
                readThread.interrupt();
            } catch (Exception e) {
                System.err.println("ConnectionOverTCP: Exception in stop() " + e);
            }
        }
    }


    /**
     * Process a control datagram
     */
    protected void processControlDatagram(Datagram dg) {
        System.out.println("Control Datagram " + dg);

        byte[] payload = dg.getPayload();

        if (payload[0] == 'C') {
            remoteClose = true;
            close();
        }

    }

    /**
     * Consturct and send a control message.
     */
    protected boolean controlClose() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("C".getBytes());
        Datagram datagram = new IPV4Datagram(buffer);
        datagram.setProtocol(1);

        return sendDatagram(datagram);

        
    }


}
