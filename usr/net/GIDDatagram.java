package usr.net;

import java.nio.ByteBuffer;
import usr.logging.*;
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

   static int initialTTL_= 64; // TTL used for new 

    /**
     * Construct a GIDDatagram given a payload.
     */
    GIDDatagram(ByteBuffer payload) {
        payload.rewind();
        int payloadSize = payload.limit();
        //Logger.getLogger("log").logln(USR.ERROR, "PAYLOAD SIZE "+payloadSize);
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
        return (byte)(CHECKSUM_SIZE & 0xFF);
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
        byte b = fullDatagram.get(8);
        return 0 | (0xFF & b);
    }
    
    /**
     * Set the TTL
     */
    public Datagram setTTL(int ttl) {
        byte b = (byte)(ttl & 0xFF);
        fullDatagram.put(8, b);
        return this;
    }

    static void setInitialTTL(int ttl) {
        initialTTL_= ttl;
    }

    /**
     * Get the protocol
     */
    public byte getProtocol() {
        byte b = fullDatagram.get(9);
        return b;
    }

    /**
     * Set the protocol
     */
    public Datagram setProtocol(int p) {
        byte b = (byte)(p & 0xFF);
        fullDatagram.put(9, b);

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
        return new GIDAddress(address);
    }

    /**
     * Set the src address
     */
    public Datagram setSrcAddress(Address addr) {
        if (addr != null && ! (addr instanceof Size4)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in GIDDatagram");
        }

        // put src addr
        fullDatagram.position(10);
        if (addr == null) {
            fullDatagram.put(GIDAddress.EMPTY, 0, 4);
        } else {
            fullDatagram.put(addr.asByteArray(), 0, 4);
        }

        return this;
    }

    /**
     * Get dst address.
     */
    public Address getDstAddress() {
        // get 4 bytes for address
        //if (dstAddr == null)
         //   return null;
        byte[] address = new byte[4];
        fullDatagram.position(14);
        fullDatagram.get(address, 0, 4);
        if (emptyAddress(address))
            return null;
        return new GIDAddress(address);
    }

    /**
     * Set the dst address
     */
    public Datagram setDstAddress(Address addr) {
        if (addr != null && ! (addr instanceof Size4)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in GIDDatagram");
        }

        dstAddr = addr;
        fullDatagram.position(14);

        // put dst addr
        // to be filled in later
        if (addr == null) {
            fullDatagram.put(GIDAddress.EMPTY, 0, 4);
        } else {
            fullDatagram.put(dstAddr.asByteArray(), 0, 4);
        }

        return this;
    }

    boolean emptyAddress(byte []address) 
    {
       for (int i= 0; i < 4; i++) {
          if (address[i] != GIDAddress.EMPTY[i])
            return false;
       }
       return true;
    }
    

    /**
     * Get src port.
     */
    public int getSrcPort() {
        int p = (int)fullDatagram.getShort(20);
        // convert signed to unsigned
        if (p < 0) {
            return p + 65536;
        } else {
            return p;
        }
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
        int p = (int)fullDatagram.getShort(22);
        // convert signed to unsigned
        if (p < 0) {
            return p + 65536;
        } else {
            return p;
        }
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

        // Logger.getLogger("log").logln(USR.ERROR, "GIDDatagram getPayload: headerLen = " + headerLen + " totalLen = " + totalLen);

        fullDatagram.position(headerLen);

        byte[] payloadBytes = new byte[totalLen - CHECKSUM_SIZE - headerLen];

        fullDatagram.get(payloadBytes);

        //ByteBuffer payload =  ByteBuffer.wrap(payloadBytes);

        // Logger.getLogger("log").logln(USR.ERROR, "GIDDatagram getPayload: payload = " + payload.position() + " < " + payload.limit() + " < " + payload.capacity());

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
        
        // Logger.getLogger("log").logln(USR.ERROR, "GIDDatagram fromByteBuffer: fullDatagram = " + fullDatagram.position() + " < " + fullDatagram.limit() + " < " + fullDatagram.capacity());

        return true;
    }

    /**
     * Fill in the Datagram with the relevant values.
     */
    void fillDatagram(ByteBuffer payload) {
        // put USRD literal - 4 bytes
        fullDatagram.put("USRD".getBytes(), 0, 4);
        //Logger.getLogger("log").logln(USR.ERROR, "USRD set");
        // put header len
        fullDatagram.put(4, (byte)(HEADER_SIZE & 0xFF));

        // put total len
        fullDatagram.putShort(5, (short)fullDatagram.capacity());

        // put flags
        byte flags = 0;
        fullDatagram.put(7, (byte)flags);

        // put ttl
        // start with default
        fullDatagram.put(8, (byte)initialTTL_);

        // protocol
        byte protocol = 0;
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
        // Logger.getLogger("log").logln(USR.ERROR, "payload size = " + payload.limit());

        fullDatagram.put(payload);

        // evaluate checksum
        int checksum = -1;
        fullDatagram.putInt(fullDatagram.capacity() - 4, checksum);

        fullDatagram.rewind();

        // Logger.getLogger("log").logln(USR.ERROR, "GIDAddress fillDatagram: fullDatagram = " + fullDatagram.position() + " < " + fullDatagram.limit() + " < " + fullDatagram.capacity());

    }

}
