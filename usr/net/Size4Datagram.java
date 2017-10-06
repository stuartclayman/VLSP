package usr.net;

import java.nio.ByteBuffer;

/**
 * An abstract Foundation class for Datagrams that use Size4 addresses.
 */
class Size4Datagram implements Datagram, DatagramPatch {
    // The full datagram is 36 bytes plus the payload
    // as an  Address is 4 bytes long
    final static int HEADER_SIZE = 36;
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
    // timestamp  - 24 / 8
    // flow ID    - 32 / 4
    // payload    - 36 / N
    // checksum   - 36 + N / 4


    // The full datagram contents
    ByteBuffer fullDatagram;

    static int initialTTL_ = 64; // TTL used for new

    /**
     * Construct a Size4Datagram given a payload.
     */
    Size4Datagram(ByteBuffer payload) {
        long timestamp = System.currentTimeMillis();

        payload.rewind();
        int payloadSize = payload.limit();
        //Logger.getLogger("log").logln(USR.ERROR, "PAYLOAD SIZE "+payloadSize);
        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload, timestamp);
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
        long timestamp = System.currentTimeMillis();

        payload.rewind();

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload, timestamp);
        setDstAddress(address);

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
        long timestamp = System.currentTimeMillis();

        payload.rewind();

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload, timestamp);
        setDstAddress(address);
        setDstPort(port);
    }

    /**
     * Construct a Size4Datagram given a payload, a destination address,
     * and a destination port.
     */
    Size4Datagram(byte[] payload, Address address, int port) {
        this(ByteBuffer.wrap(payload), address, port);
    }

    /**
     * Construct a Size4Datagram given a payload, a destination address,
     * and a destination port.
     */
    Size4Datagram(byte[] payload, int len, Address address, int port) {
        this(ByteBuffer.wrap(payload), address, port);
    }

    Size4Datagram() {
    }

    /**
     * Get the length of the data, i.e. the payload length.
     */
    @Override
    public synchronized int getLength() {
        return getTotalLength() - getHeaderLength() - getChecksumLength();
    }

    /**
     * Get the header len
     */
    @Override
    public synchronized byte getHeaderLength() {
        // return HEADER_SIZE;
        return fullDatagram.get(4);
    }

    /**
     * Get the total len
     */
    @Override
    public synchronized short getTotalLength() {
        return fullDatagram.getShort(5);
    }

    /**
     * Get the checksum size
     */
    @Override
    public synchronized byte getChecksumLength() {
        return (byte)(CHECKSUM_SIZE & 0xFF);
    }

    /**
     * Get the Timestamp.
     * The time the Datagram was created.
     */
    @Override
    public synchronized long getTimestamp() {
        long ts = fullDatagram.getLong(24);

        return ts;
    }

    /**
     * Get the flags
     */
    @Override
    public synchronized byte getFlags() {
        return fullDatagram.get(7);
    }

    /**
     * Get the TTL
     */
    @Override
    public synchronized int getTTL() {
        byte b = fullDatagram.get(8);
        return 0 | (0xFF & b);
    }

    /**
     * Set the TTL
     */
    @Override
    public synchronized Datagram setTTL(int ttl) {
        byte b = (byte)(ttl & 0xFF);
        fullDatagram.put(8, b);
        return this;
    }

    /**
     * Get the protocol
     */
    @Override
    public synchronized byte getProtocol() {
        byte b = fullDatagram.get(9);
        return b;
    }

    /**
     * Set the protocol
     */
    @Override
    public synchronized Datagram setProtocol(int p) {
        byte b = (byte)(p & 0xFF);
        fullDatagram.put(9, b);

        return this;
    }

    /**
     * Get src address.
     */
    @Override
    public synchronized Address getSrcAddress() {
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
    public synchronized Datagram setSrcAddress(Address addr) {
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
    public synchronized Address getDstAddress() {
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
    public synchronized Datagram setDstAddress(Address addr) {
        if (addr != null && !(addr instanceof Size4)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in Size4Datagram");
        }

        // put dst addr
        fullDatagram.position(14);

        if (addr == null) {
            fullDatagram.put(Size4.EMPTY, 0, 4);
        } else {
            fullDatagram.put(addr.asByteArray(), 0, 4);
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
    public synchronized int getSrcPort() {
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
    @Override
    public synchronized Datagram setSrcPort(int p) {
        fullDatagram.putShort(20, (short)p);

        return this;
    }

    /**
     * Get dst port.
     */
    @Override
    public synchronized int getDstPort() {
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
    @Override
    public synchronized Datagram setDstPort(int p) {
        fullDatagram.putShort(22, (short)p);

        return this;
    }


    /**
     * Get the Socket Address of the src.
     */
    public synchronized SocketAddress getSocketAddress() {
        return new SocketAddress(getSrcAddress(), getSrcPort());
    }

    /**
     * Get the flow ID
     */
    @Override
    public synchronized int getFlowID() {
        int f = fullDatagram.getInt(32);
        return f;
    }

    /**
     * Set the flow iD
     */
    @Override
    public synchronized Datagram setFlowID(int id) {
        fullDatagram.putInt(32, id);

        return this;
    }


    /** Reduce TTL and return true if packet still valid */
    @Override
    public synchronized boolean TTLReduce() {
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
    public synchronized byte[] getHeader() {
        int headerLen = getHeaderLength();

        fullDatagram.position(0);

        byte[] headerBytes = new byte[headerLen];

        fullDatagram.get(headerBytes);

        fullDatagram.rewind();

        return headerBytes;
    }

    /**
     * Get payload as a byte[] copy
     */
    @Override
    public synchronized byte[] getPayload() {
        fullDatagram.position(0);

        int headerLen = getHeaderLength();
        int totalLen = getTotalLength();

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Datagram getPayload: headerLen = " + headerLen + " totalLen = " + totalLen);

        fullDatagram.position(headerLen);

        byte[] payloadBytes = new byte[totalLen - CHECKSUM_SIZE - headerLen];

        fullDatagram.get(payloadBytes);

        //ByteBuffer payload =  ByteBuffer.wrap(payloadBytes);

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Datagram getPayload: payload = " + payload.position() + " < " +  payload.limit() + " < " + payload.capacity());

        fullDatagram.rewind();

        return payloadBytes;

    }

    /**
     * View payload as a constituent part of a Datagram
     */
    protected ByteBuffer viewPayload() {
        fullDatagram.position(getHeaderLength());
        fullDatagram.limit(getTotalLength() - CHECKSUM_SIZE);

        ByteBuffer slice =  fullDatagram.slice();

        /*
        System.err.println("sliceBuffer SB(P) = " + slice.position() +
                           " SB(C) = " + slice.capacity() +
                           " B(P) = " + fullDatagram.position() + 
                           " B(L) = " + fullDatagram.limit() +
                           " B(C) = " + fullDatagram.capacity());
        */

        fullDatagram.rewind();

        return slice;

    }

    /**
     * Get payload
     */
    @Override
    public synchronized byte[] getData() {
        return getPayload();
    }

    /**
     * Get the checksum
     */
    @Override
    public synchronized byte[] getChecksum() {
        int checksumLen = getChecksumLength();
        int totalLen = getTotalLength();

        fullDatagram.position(totalLen - totalLen);

        byte [] checksumBytes = new byte[checksumLen];

        fullDatagram.get(checksumBytes);

        fullDatagram.rewind();

        return checksumBytes;
    }

    /**
     * To ByteBuffer.
     */
    @Override
    public synchronized ByteBuffer toByteBuffer() {
        fullDatagram.rewind();
        return fullDatagram;
    }

    /**
     * From ByteBuffer.
     */
    @Override
    public synchronized boolean fromByteBuffer(ByteBuffer b) {
        fullDatagram = b;

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Datagram fromByteBuffer: fullDatagram = " + fullDatagram.position() + " <
        // " + fullDatagram.limit() + " < " + fullDatagram.capacity());

        return true;
    }

    /**
     * Fill in the Datagram with the relevant values.
     */
    void fillDatagram(ByteBuffer payload, long timestamp) {
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
        fullDatagram.put(Size4.EMPTY, 0, 4);

        // put dst addr
        // to be filled in later
        fullDatagram.position(14);

        fullDatagram.put(Size4.EMPTY, 0, 4);

        // 2 spare bytes
        fullDatagram.put(18, (byte)0);
        fullDatagram.put(19, (byte)0);

        // put src port
        fullDatagram.putShort(20, (short)0);

        // put dst port
        fullDatagram.putShort(22, (short)0);

        // put timestamp
        fullDatagram.putLong(24, timestamp);

        // put flowID
        // to be filled in later
        fullDatagram.position(32);
        fullDatagram.putInt(0);


        /*
         * copy in payload
         */
        fullDatagram.position(36);
        payload.rewind();
        // Logger.getLogger("log").logln(USR.ERROR, "payload size = " + payload.limit());

        fullDatagram.put(payload.array(), 0, payload.limit());

        // evaluate checksum
        int checksum = -1;
        fullDatagram.putInt(fullDatagram.capacity() - 4, checksum);

        // Logger.getLogger("log").logln(USR.ERROR, "Size4Address fillDatagram: fullDatagram = " + fullDatagram.position() + " < " +
        // fullDatagram.limit() + " < " + fullDatagram.capacity());

        fullDatagram.rewind();
    }

    /**
     * Get a copy of the Datagram
     */
    public Object clone() throws CloneNotSupportedException {
        return DatagramFactory.copy(this);
    }


    /**
     * To String
     */
    @Override
    public String toString() {
        /*
        return "( src: " + getSrcAddress() + "/" + getSrcPort() +
               " dst: " + getDstAddress() + "/" + getDstPort() +
               " len: " + getTotalLength() + " proto: " + getProtocol() +
               " ttl: " + getTTL() + " payload: " + (getTotalLength() - HEADER_SIZE - CHECKSUM_SIZE) +
               " )";
        */

        return "id: "+ System.identityHashCode(this)  + " ( position = " + fullDatagram.position() + 
            " limit = " + fullDatagram.limit() + " capacity = " + fullDatagram.capacity() + " )";
    }

}
