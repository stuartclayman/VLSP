package usr.net;

import java.net.InetAddress;

public interface Address {
    /**
     * Get the size in bytes of an instantiation of an Address.
     */
    public int size();

    /**
     * Get Address as a byte[]
     */
    public byte[] asByteArray();

    /**
     * Get IPV4Address as an InetAddress
     * @throws UnsupportedOperationException if the actual kind of address
     * cannot be specified as an InetAddress
     */
    public InetAddress asInetAddress();
} 
