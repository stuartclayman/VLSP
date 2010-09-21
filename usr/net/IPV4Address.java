package usr.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * An IPV4 Address
 */
public class IPV4Address implements Address {
    byte[] bytes = new byte[4];
    InetAddress addr;

    // an EMPTY address
    public final static byte[] EMPTY = { 0, 0, 0, 0};


    /**
     * Create a IPV4Address from a hostname
     */
    public IPV4Address(String hostname) throws UnknownHostException {
        addr = InetAddress.getByName(hostname);
        bytes = addr.getAddress();
    }

    /**
     * Create a IPV4Address from a byte[]
     */
    public IPV4Address(byte[] addr)  throws UnknownHostException {
        if (addr.length == 4) {
            bytes = addr;
            this.addr = InetAddress.getByAddress(bytes);
        } else {
            throw new UnknownHostException("InetAddress: wrong length. Expected 4, got " + addr.length);
        }
    }

    /**
     * Get IPV4Address as a byte[]
     */
    public byte[] asByteArray() {
        return bytes;
    }

    /**
     * Get the size in bytes of an instantiation of an IPV4Address.
     */
    public int size() {
        return 4;
    }
    
    /**
     * Equals
     */
    public boolean equals(Object addr) {
        if (addr instanceof Address) {
            return addr.toString().equals(toString());
        } else {
            return false;
        }
   }

    /**
     * Get IPV4Address as an InetAddress
     */
    public InetAddress asInetAddress() {
        return addr;
    }

    /**
     * To String
     */
    public String toString() {
        return numericToTextFormat(bytes);
    }

    /**
     * Convert.
     */
    static String numericToTextFormat(byte[] src)
    {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
    }

}

