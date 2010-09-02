package usr.net;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Set a UDP so that it sends data as USR Datagrams.
 */
public class DatagramUDP {
    // The underlying Socket
    DatagramSocket socket;

    // The local address for this connection
    Address localAddress;

    // port no at remote end
    int port;

    // A ByteBuffer to read into
    int BUF_SIZE = 2048;
    ByteBuffer buffer;

    // reveice array
    byte[] recvArray;
    DatagramPacket recvPacket;

    // current position in the ByteBuffer
    int current = 0;

    /**
     * Construct a DatagramUDP given a DatagramSocket.
     */
    public DatagramUDP(DatagramSocket s) {
        socket = s; 
        recvArray = new byte[BUF_SIZE];
        recvPacket = new DatagramPacket(recvArray, recvArray.length);
        buffer = ByteBuffer.wrap(recvArray);
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
    public DatagramUDP setAddress(Address addr) {
        localAddress = addr;
        return this;
    }

    /**
     * Get the port no for this connection.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port no. for this connection.
     */
    public DatagramUDP setPort(int port) {
        this.port = port;
        return this;
    }


    /**
     * Send a Datagram.
     */
    public boolean sendDatagram(Datagram dg) {
        // set the source address and port on the Datagram
        dg.setSrcAddress(localAddress);

        try {
            // convert byte buffer to a DatagramPacket
            byte[] data = ((DatagramPatch)dg).toByteBuffer().array();
            DatagramPacket packet = new DatagramPacket(data, data.length);
            InetAddress addr = localAddress.asInetAddress();
            packet.setPort(port);
            packet.setAddress(addr);

            //System.err.println("DatagramUDP sendDatagram: packet = " + packet);
            
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

                // System.err.println("DatagramUDP readDatagram: buffer = " + buffer.position() + " < " + buffer.limit() + " < " + buffer.capacity());

                totalLen = (short)count;

                // now get Datagram
                byte[] latestDGData = new byte[totalLen];
                buffer.get(latestDGData);

                Datagram dg = new IPV4Datagram();                        
                ByteBuffer newBB = ByteBuffer.wrap(latestDGData);
                ((DatagramPatch)dg).fromByteBuffer(newBB);

                buffer.clear();

                return dg;

            } catch (IOException ioe) {
                // TODO:  return Datagram.ERROR object
                return null;
            }
    }

    /**
     * Get the DatagramSocket.
     */
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * To String
     */
    public String toString() {
        return socket.toString();
    }



}
