package usr.net;

import java.net.InetAddress;
import usr.logging.*;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.io.Serializable;

/**
 * An IPV4 Address
 */
public class IPV4Address extends Size4 implements Address, Serializable {
    InetAddress addr;

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
     * Create an IPV4Address from an int
     */
    public IPV4Address(int addr) {
         // convert int to byte[] 
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putInt(addr);
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
     * Get IPV4Address as an InetAddress
     */
    public InetAddress asInetAddress() {
        return addr;
    }

    /** 
     * Get IPV4Address as an Integer.
     */
    public int asInteger() {
        // convert byte[] to int
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return buf.getInt();
    }

    /**
     * Compare this IPV4Address to another one
     */
    public int compareTo(Object other) { 
        int val1 = this.asInteger();
        int val2 = ((Address)other).asInteger();

        if (val1 == val2) {
            return 0;
        } else if (val1 < val2) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof Address) {
            Address addr = (Address)obj;
            return addr.asInteger() == this.asInteger();
        } else {
            return false;
        }
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

