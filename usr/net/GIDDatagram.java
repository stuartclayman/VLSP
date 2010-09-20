package usr.net;

import java.nio.ByteBuffer;
import java.net.UnknownHostException;

/**
 * A simple implementation of a GID Datagram.
 */
public class GIDDatagram implements Datagram, DatagramPatch {
    // The full datagram is 24 bytes plus the payload
    // as an GID Address is 4 bytes long
    final static int HEADER_SIZE = 24;
    final static int CHECKSUM_SIZE = 4;

    // The full datagram contents
    ByteBuffer fullDatagram;

    // Dst address
    Address dstAddr = null;

    // Dst port
    int dstPort = 0;



    /**
     * Construct a GIDDatagram given a payload.
     */
    GIDDatagram(ByteBuffer payload) {
        payload.rewind();
        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a GIDDatagram given a payload and a destination address
     */
    GIDDatagram(ByteBuffer payload, Address address) {
        payload.rewind();
        dstAddr = address;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a GIDDatagram given a payload, a destination address,
     * and a destination port.
     */
    GIDDatagram(ByteBuffer payload, Address address, int port) {
        payload.rewind();
        dstAddr = address;
        dstPort = port;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    GIDDatagram() {
    }

    /**
     * Get the header len
     */
    public byte getHeaderLength() {
        // return HEADER_SIZE;
        return fullDatagram.get(4);
    }

    /**
     * Get the total len
     */
    public short getTotalLength() {
        return fullDatagram.getShort(5);
    }

    /**
     * Get the checksum size
     */
    public byte getChecksumLength() {
        return (byte)CHECKSUM_SIZE;
    }


    /**
     * Get the flags
     */
    public byte getFlags() {
        return fullDatagram.get(7);
    }

    /**
     * Get the TTL
     */
    public int getTTL() {
        return fullDatagram.get(8);
    }

    /**
     * Get the protocol
     */
    public byte getProtocol() {
        return fullDatagram.get(9);
    }

    /**
     * Set the protocol
     */
    public Datagram setProtocol(int p) {
        fullDatagram.put(9, (byte)p);

        return this;
    }


    /**
     * Get src address.
     */
    public Address getSrcAddress() {
        // get 4 bytes for address
        byte[] address = new byte[4];
        fullDatagram.position(10);
        fullDatagram.get(address, 0, 4);

        return new GIDAddress(address);
    }

    /**
     * Set the src address
     */
    public Datagram setSrcAddress(Address addr) {
        if (addr != null && ! (addr instanceof GIDAddress)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in GIDDatagram");
        }

        // put src addr
        if (addr == null) {
            fullDatagram.put(GIDAddress.EMPTY, 0, 4);
        } else {
            fullDatagram.position(10);
            fullDatagram.put(addr.asByteArray(), 0, 4);
        }

        return this;
    }

    /**
     * Get dst address.
     */
    public Address getDstAddress() {
        // get 4 bytes for address
        byte[] address = new byte[4];
        fullDatagram.position(14);
        fullDatagram.get(address, 0, 4);

        return new GIDAddress(address);
    }

    /**
     * Set the dst address
     */
    public Datagram setDstAddress(Address addr) {
        if (addr != null && ! (addr instanceof GIDAddress)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in GIDDatagram");
        }

        dstAddr = addr;

        // put dst addr
        // to be filled in later
        fullDatagram.position(14);
        if (dstAddr == null) {
            fullDatagram.put(GIDAddress.EMPTY, 0, 4);
        } else {
            fullDatagram.put(dstAddr.asByteArray(), 0, 4);
        }

        return this;
    }


    /**
     * Get src port.
     */
    public int getSrcPort() {
        return (int)fullDatagram.getShort(20);
    }

    /**
     * Set the src port
     */
    public Datagram setSrcPort(int p) {
        fullDatagram.putShort(20, (short)p);

        return this;
    }

    /**
     * Get dst port.
     */
    public int getDstPort() {
        return (int)fullDatagram.getShort(22);
    }

    /**
     * Set the dst port
     */
    public Datagram setDstPort(int p) {
        dstPort = p;
        fullDatagram.putShort(22, (short)dstPort);

        return this;
    }

    /**
     * Get header
     */
    public byte[] getHeader() {
        int headerLen = getHeaderLength();

        fullDatagram.position(0);

        byte[] headerBytes = new byte[headerLen];

        fullDatagram.get(headerBytes);

        return headerBytes;
    }

    /**
     * Get payload
     */
    public byte[] getPayload() {
        int headerLen = getHeaderLength();
        int totalLen = getTotalLength();

        // System.err.println("GIDDatagram getPayload: headerLen = " + headerLen + " totalLen = " + totalLen);

        fullDatagram.position(headerLen);

        byte[] payloadBytes = new byte[totalLen - CHECKSUM_SIZE - headerLen];

        fullDatagram.get(payloadBytes);

        //ByteBuffer payload =  ByteBuffer.wrap(payloadBytes);

        // System.err.println("GIDDatagram getPayload: payload = " + payload.position() + " < " + payload.limit() + " < " + payload.capacity());

        return payloadBytes;
        
    }

    /**
     * Get the checksum
     */
    public byte[] getChecksum() {
        int checksumLen = getChecksumLength();
        int totalLen = getTotalLength();

        fullDatagram.position(totalLen - totalLen);

        byte [] checksumBytes = new byte[checksumLen];

        fullDatagram.get(checksumBytes);

        return checksumBytes;
    }

    /**
     * To ByteBuffer.
     */
    public ByteBuffer toByteBuffer() {
        fullDatagram.rewind();
        return fullDatagram;
    }

    /**
     * From ByteBuffer.
     */
    public boolean fromByteBuffer(ByteBuffer b) {
        fullDatagram = b;

        // System.err.println("GIDDatagram fromByteBuffer: fullDatagram = " + fullDatagram.position() + " < " + fullDatagram.limit() + " < " + fullDatagram.capacity());

        return true;
    }

    /**
     * Fill in the Datagram with the relevant values.
     */
    void fillDatagram(ByteBuffer payload) {
        // put USRD literal - 4 bytes
        fullDatagram.put("USRD".getBytes(), 0, 4);

        // put header len
        fullDatagram.put(4, (byte)HEADER_SIZE);

        // put total len
        fullDatagram.putShort(5, (short)fullDatagram.capacity());

        // put flags
        int flags = 0;
        fullDatagram.put(7, (byte)flags);

        // put ttl
        int ttl = 0;
        fullDatagram.put(8, (byte)ttl);

        // protocol
        int protocol = 0;
        fullDatagram.put(9, (byte)protocol);

        // put src addr
        fullDatagram.position(10);
        fullDatagram.put(GIDAddress.EMPTY, 0, 4);

        // put dst addr
        // to be filled in later
        fullDatagram.position(14);
        if (dstAddr == null) {
            fullDatagram.put(GIDAddress.EMPTY, 0, 4);
        } else {
            fullDatagram.put(dstAddr.asByteArray(), 0, 4);
        }

        // 2 spare bytes
        fullDatagram.put(18, (byte)0);
        fullDatagram.put(19, (byte)0);

        // put src port
        fullDatagram.putShort(20, (short)0);

        // put dst port
        // to be filled in later
        fullDatagram.putShort(22, (short)dstPort);

        /*
         * copy in payload
         */
        fullDatagram.position(24);
        payload.rewind();
        // System.err.println("payload size = " + payload.limit());

        fullDatagram.put(payload);

        // evaluate checksum
        int checksum = -1;
        fullDatagram.putInt(fullDatagram.capacity() - 4, checksum);

        fullDatagram.rewind();

        // System.err.println("GIDAddress fillDatagram: fullDatagram = " + fullDatagram.position() + " < " + fullDatagram.limit() + " < " + fullDatagram.capacity());

    }

}
