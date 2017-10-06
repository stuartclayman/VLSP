package usr.net;

import usr.net.Address;

/**
 * A Datagram.
 */
public interface Datagram extends Cloneable {
    /**
     * Get the length of the data, i.e. the payload length.
     */
    public int getLength();

    /**
     * Get the header len
     */
    public byte getHeaderLength();

    /**
     * Get the total len
     */
    public short getTotalLength();

    /**
     * Get the checksum size
     */
    public byte getChecksumLength();

    /**
     * Get the Timestamp.
     * The time the Datagram was created.
     */
    public long getTimestamp();

    /**
     * Get the flags
     */
    public byte getFlags();

    /**
     * Get the TTL
     */
    public int getTTL();

    /**
     * Set the TTL
     */
    public Datagram setTTL(int ttl);

    /**
     * Get the protocol
     */
    public byte getProtocol();

    /**
     * Set the protocol
     */
    public Datagram setProtocol(int p);

    /**
     * Get the Socket Address of the src.
     */
    public SocketAddress getSocketAddress();

    /**
     * Get src address.
     */
    public Address getSrcAddress();

    /**
     * Set the src address
     */
    public Datagram setSrcAddress(Address addr);

    /**
     * Get dst address.
     */
    public Address getDstAddress();

    /**
     * Set the dst address
     */
    public Datagram setDstAddress(Address addr);

    /**
     * Get src port.
     */
    public int getSrcPort();

    /**
     * Set the src port
     */
    public Datagram setSrcPort(int port);

    /**
     * Get dst port.
     */
    public int getDstPort();

    /**
     * Set the dst port
     */
    public Datagram setDstPort(int port);

    /**
     * Get the flow ID
     */
    public int getFlowID();

    /**
     * Set the flow iD
     */
    public Datagram setFlowID(int id);

    /** Reduce TTL and return true if packet still valid */
    public boolean TTLReduce();

    /**
     * Get header
     */
    public byte[] getHeader();

    /**
     * Get payload
     */
    public byte[] getPayload();

    /**
     * Get payload
     */
    public byte[] getData();

    /**
     * Get the checksum
     */
    public byte[] getChecksum();


    /**
     * Get a copy of the Datagram
     */
    public Object clone() throws CloneNotSupportedException ;


}
