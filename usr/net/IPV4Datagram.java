package usr.net;

import java.nio.ByteBuffer;
import usr.logging.*;
import java.net.UnknownHostException;

/**
 * A simple implementation of a IPV4 Datagram.
 */
public class IPV4Datagram implements Datagram, DatagramPatch {
    // The full datagram is 24 bytes plus the payload
    // as an IPV4 Address is 4 bytes long
    final static int HEADER_SIZE = 24;
    final static int CHECKSUM_SIZE = 4;
    


    // The full datagram contents
    ByteBuffer fullDatagram;

    // Src address
    Address srcAddr = null;

    // Src port
    int srcPort = 0;

    // Dst address
    Address dstAddr = null;

    // Dst port
    int dstPort = 0;

    static int initialTTL_= 64;

    /**
     * Construct a IPV4Datagram given a payload.
     */
    IPV4Datagram(ByteBuffer payload) {
        payload.rewind();
        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a IPV4Datagram given a payload and a destination address
     */
    IPV4Datagram(ByteBuffer payload, Address address) {
        payload.rewind();
        dstAddr = address;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a IPV4Datagram given a payload, a destination address,
     * and a destination port.
     */
    IPV4Datagram(ByteBuffer payload, Address address, int port) {
        payload.rewind();
        dstAddr = address;
        dstPort = port;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    IPV4Datagram() {
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
     * Set the TTL
     */
    public void setTTL(int ttl) {
        fullDatagram.put(8,(byte)ttl);
    }

    /** Set the default TTL to be used by new packets */
    static void setInitialTTL(int ttl) {
        initialTTL_= ttl;
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
        if (emptyAddress(address))
            return null;
        try {
            return new IPV4Address(address);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

    /**
     * Set the src address
     */
    public Datagram setSrcAddress(Address addr) {
        srcAddr = addr;

        // put src addr
        // to be filled in later
        fullDatagram.position(10);
        if (srcAddr == null) {
            fullDatagram.put(IPV4Address.EMPTY, 0, 4);
        } else {
            fullDatagram.put(srcAddr.asByteArray(), 0, 4);
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
        if (emptyAddress(address))
            return null;
        try {
            return new IPV4Address(address);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }


    boolean emptyAddress(byte []address) 
    {
       for (int i= 0; i < 4; i++) {
          if (address[i] != IPV4Address.EMPTY[i])
            return false;
       }
       return true;
    }
    /**
     * Set the dst address
     */
    public Datagram setDstAddress(Address addr) {
        dstAddr = addr;

        // put dst addr
        // to be filled in later
        fullDatagram.position(14);
        if (dstAddr == null) {
            fullDatagram.put(IPV4Address.EMPTY, 0, 4);
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
        srcPort = p;
        fullDatagram.putShort(20, (short)srcPort);

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
    
     /** Reduce TTL and return true if packet still valid */
    public boolean TTLReduce() {
        int ttl= getTTL();
        ttl--;
        setTTL(ttl);
        if (ttl <= 0)
            return false;
        return true;
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

        // Logger.getLogger("log").logln(USR.ERROR, "IPV4Datagram getPayload: headerLen = " + headerLen + " totalLen = " + totalLen);

        fullDatagram.position(headerLen);

        byte[] payloadBytes = new byte[totalLen - CHECKSUM_SIZE - headerLen];

        fullDatagram.get(payloadBytes);

        //ByteBuffer payload =  ByteBuffer.wrap(payloadBytes);

        // Logger.getLogger("log").logln(USR.ERROR, "IPV4Datagram getPayload: payload = " + payload.position() + " < " + payload.limit() + " < " + payload.capacity());

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

        // Logger.getLogger("log").logln(USR.ERROR, "IPV4Datagram fromByteBuffer: fullDatagram = " + fullDatagram.position() + " < " + fullDatagram.limit() + " < " + fullDatagram.capacity());

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
        fullDatagram.put(8, (byte)initialTTL_);

        // protocol
        int protocol = 0;
        fullDatagram.put(9, (byte)protocol);

        // put src addr
        // to be filled in later
        fullDatagram.position(10);
        if (srcAddr == null) {
            fullDatagram.put(IPV4Address.EMPTY, 0, 4);
        } else {
            fullDatagram.put(srcAddr.asByteArray(), 0, 4);
        }

        // put dst addr
        // to be filled in later
        fullDatagram.position(14);
        if (dstAddr == null) {
            fullDatagram.put(IPV4Address.EMPTY, 0, 4);
        } else {
            fullDatagram.put(dstAddr.asByteArray(), 0, 4);
        }

        // 2 spare bytes
        fullDatagram.put(18, (byte)0);
        fullDatagram.put(19, (byte)0);

        // put src port
        // to be filled in later
        fullDatagram.putShort(20, (short)srcPort);

        // put dst port
        // to be filled in later
        fullDatagram.putShort(22, (short)dstPort);

        /*
         * copy in payload
         */
        fullDatagram.position(24);
        payload.rewind();
        // Logger.getLogger("log").logln(USR.ERROR, "payload size = " + payload.limit());

        fullDatagram.put(payload);

        // evaluate checksum
        int checksum = -1;
        fullDatagram.putInt(fullDatagram.capacity() - 4, checksum);

        fullDatagram.rewind();

        // Logger.getLogger("log").logln(USR.ERROR, "IPV4Address fillDatagram: fullDatagram = " + fullDatagram.position() + " < " + fullDatagram.limit() + " < " + fullDatagram.capacity());

    }

}
