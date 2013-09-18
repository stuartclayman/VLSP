package usr.net;

import java.nio.ByteBuffer;

/**
 * An abstract Foundation class for Datagrams that use Size4 addresses.
 */
class Size4Datagram implements Datagram, DatagramPatch {
    // The full datagram is 24 bytes plus the payload
    // as an  Address is 4 bytes long
    final static int HEADER_SIZE = 24;
    final static int CHECKSUM_SIZE = 4;

    // USRD       - 0 / 4
    // hdr size   - 4 / 1
    // total len  - 5 / 2
    // flags      - 7 / 1
    // ttl        - 8 / 1
    // protocol   - 9 / 1
    // src addr   - 10 / 4
    // dst addr   - 14 / 4
    // spare      - 18 / 1
    // spare      - 19 / 1
    // src port   - 20 / 2
    // dst port   - 22 / 2
    // payload    - 24 / N
    // checksum   - 24 + N / 4


    // The full datagram contents
    ByteBuffer fullDatagram;

    // Dst address
    Address dstAddr = null;

    // Dst port
    int dstPort = 0;

    static int initialTTL_ = 64; // TTL used for new

    /**
     * Construct a Size4Datagram given a payload.
     */
    Size4Datagram(ByteBuffer payload) {
        payload.rewind();
        int payloadSize = payload.limit();
        //Logger.getLogger("log").logln(USR.ERROR, "PAYLOAD SIZE "+payloadSize);
        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a Size4Datagram given a payload.
     */
    Size4Datagram(byte[] payload) {
        this(ByteBuffer.wrap(payload));
    }

    /**
     * Construct a Size4Datagram given a payload and a destination address
     */
    Size4Datagram(ByteBuffer payload, Address address) {
        payload.rewind();
        dstAddr = address;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a Size4Datagram given a payload and a destination address
     */
    Size4Datagram(byte[] payload, Address address) {
        this(ByteBuffer.wrap(payload), address);
    }

    /**
     * Construct a Size4Datagram given a payload, a destination address,
     * and a destination port.
     */
    Size4Datagram(ByteBuffer payload, Address address, int port) {
        payload.rewind();
        dstAddr = address;
        dstPort = port;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a Size4Datagram given a payload, a destination address,
     * and a destination port.
     */
    Size4Datagram(byte[] payload, Address address, int port) {
        this(ByteBuffer.wrap(payload), address, port);
    }

    Size4Datagram() {
    }

    /**
     * Get the length of the data, i.e. the payload length.
     */
    @Override
	public int getLength() {
        return getTotalLength() - getHeaderLength() - getChecksumLength();
    }

    /**
     * Get the header len
     */
    @Override
	public byte getHeaderLength() {
        // return HEADER_SIZE;
        return fullDatagram.get(4);
    }

    /**
     * Get the total len
     */
    @Override
	public short getTotalLength() {
        return fullDatagram.getShort(5);
    }

    /**
     * Get the checksum size
     */
    @Override
	public byte getChecksumLength() {
        return (byte)(CHECKSUM_SIZE & 0xFF);
    }

    /**
     * Get the flags
     */
    @Override
	public byte getFlags() {
        return fullDatagram.get(7);
    }

    /**
     * Get the TTL
     */
    @Override
	public int getTTL() {
        byte b = fullDatagram.get(8);
        return 0 | (0xFF & b);
    }

    /**
     * Set the TTL
     */
    @Override
	public Datagram setTTL(int ttl) {
        byte b = (byte)(ttl & 0xFF);
        fullDatagram.put(8, b);
        return this;
    }

    /**
     * Get the protocol
     */
    @Override
	public byte getProtocol() {
        byte b = fullDatagram.get(9);
        return b;
    }

    /**
     * Set the protocol
     */
    @Override
	public Datagram setProtocol(int p) {
        byte b = (byte)(p & 0xFF);
        fullDatagram.put(9, b);

        return this;
    }

    /**
     * Get src address.
     */
    @Override
	public Address getSrcAddress() {
        // get 4 bytes for address
        byte[] address = new byte[4];
        fullDatagram.position(10);
        fullDatagram.get(address, 0, 4);

        if (emptyAddress(address)) {
            return null;
        } else {
            return AddressFactory.newAddress(address);
        }
    }

    /**
     * Set the src address
     */
    @Override
	public Datagram setSrcAddress(Address addr) {
        if (addr != null && !(addr instanceof Size4)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in Size4Datagram");
        }

        // put src addr
        fullDatagram.position(10);

        if (addr == null) {
            fullDatagram.put(Size4.EMPTY, 0, 4);
        } else {
            fullDatagram.put(addr.asByteArray(), 0, 4);
        }

        return this;
    }

    /**
     * Get dst address.
     */
    @Override
	public Address getDstAddress() {
        // get 4 bytes for address
        byte[] address = new byte[4];
        fullDatagram.position(14);
        fullDatagram.get(address, 0, 4);

        if (emptyAddress(address)) {
            return null;
        } else {
            return AddressFactory.newAddress(address);
        }
    }

    /**
     * Set the dst address
     */
    @Override
	public Datagram setDstAddress(Address addr) {
        if (addr != null && !(addr instanceof Size4)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in Size4Datagram");
        }

        dstAddr = addr;
        fullDatagram.position(14);

        // put dst addr
        // to be filled in later
        if (addr == null) {
            fullDatagram.put(Size4.EMPTY, 0, 4);
        } else {
            fullDatagram.put(dstAddr.asByteArray(), 0, 4);
        }

        return this;
    }

    boolean emptyAddress(byte [] address) {
        for (int i = 0; i < 4; i++) {
            if (address[i] != Size4.EMPTY[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get src port.
     */
    @Override
	public int getSrcPort() {
        int p = fullDatagram.getShort(20);

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
    @Override
	public Datagram setSrcPort(int p) {
        fullDatagram.putShort(20, (short)p);

        return this;
    }

    /**
     * Get dst port.
     */
    @Override
	public int getDstPort() {
        int p = fullDatagram.getShort(22);

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
    @Override
	public Datagram setDstPort(int p) {
        dstPort = p;
        fullDatagram.putShort(22, (short)dstPort);

        return this;
    }

    /** Reduce TTL and return true if packet still valid */
    @Override
	public boolean TTLReduce() {
        int ttl = getTTL();
        ttl--;
        setTTL(ttl);

        if (ttl <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Get header
     */
    @Override
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
    @Override
	public byte[] getPayload() {
        int headerLen = getHeaderLength();
        int totalLen = getTotalLength();

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Datagram getPayload: headerLen = " + headerLen + " totalLen = " +
        // totalLen);

        fullDatagram.position(headerLen);

        byte[] payloadBytes = new byte[totalLen - CHECKSUM_SIZE - headerLen];

        fullDatagram.get(payloadBytes);

        //ByteBuffer payload =  ByteBuffer.wrap(payloadBytes);

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Datagram getPayload: payload = " + payload.position() + " < " +
        // payload.limit() + " < " + payload.capacity());

        return payloadBytes;

    }

    /**
     * Get the checksum
     */
    @Override
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
    @Override
	public ByteBuffer toByteBuffer() {
        fullDatagram.rewind();
        return fullDatagram;
    }

    /**
     * From ByteBuffer.
     */
    @Override
	public boolean fromByteBuffer(ByteBuffer b) {
        fullDatagram = b;

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Datagram fromByteBuffer: fullDatagram = " + fullDatagram.position() + " <
        // " + fullDatagram.limit() + " < " + fullDatagram.capacity());

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
        fullDatagram.put(7, flags);

        // put ttl
        // start with default
        fullDatagram.put(8, (byte)initialTTL_);

        // protocol
        byte protocol = 0;
        fullDatagram.put(9, protocol);

        // put src addr
        fullDatagram.position(10);
        fullDatagram.put(Size4.EMPTY, 0, 4);

        // put dst addr
        // to be filled in later
        fullDatagram.position(14);

        if (dstAddr == null) {
            fullDatagram.put(Size4.EMPTY, 0, 4);
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

        fullDatagram.put(payload.array(), 0, payload.limit());

        // evaluate checksum
        int checksum = -1;
        fullDatagram.putInt(fullDatagram.capacity() - 4, checksum);

        fullDatagram.rewind();

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Address fillDatagram: fullDatagram = " + fullDatagram.position() + " < " +
        // fullDatagram.limit() + " < " + fullDatagram.capacity());

    }

    /**
     * To String
     */
    @Override
	public String toString() {
        return "( src: " + getSrcAddress() + "/" + getSrcPort() +
               " dst: " + getDstAddress() + "/" + getDstPort() +
               " len: " + getTotalLength() + " proto: " + getProtocol() +
               " ttl: " + getTTL() + " payload: " + (getTotalLength() - HEADER_SIZE - CHECKSUM_SIZE) +
               " )";
    }

}
