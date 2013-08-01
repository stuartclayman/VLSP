package usr.net;

import usr.logging.*;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.io.Serializable;

/**
 * An IPV4 Address
 */
public class IPV6Address extends Size16 implements Address, Serializable {
    InetAddress addr;

    /**
     * Create a IPV6Address from a hostname
     */
    public IPV6Address(String hostname) throws UnknownHostException {
        this.addr = InetAddress.getByName(hostname);
        byte[] inetbytes = addr.getAddress();
        // copy bytes in
        System.arraycopy(inetbytes, 0, bytes, 0, 16);
    }

    /**
     * Create a IPV6Address from a byte[]
     */
    public IPV6Address(byte[] addr)  throws UnknownHostException {
        if (addr.length == 16) {
            // copy bytes in
            System.arraycopy(addr, 0, bytes, 0, 16);
            this.addr = InetAddress.getByAddress(bytes);
        } else {
            throw new UnknownHostException("InetAddress: wrong length. Expected 16, got " + addr.length);
        }
    }

    /**
     * Create an IPV6Address from an int
     */
    public IPV6Address(int addr) throws UnknownHostException {
        // convert int to byte[]
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.position(12);
        buf.put((byte)(addr >> 24 & 0xFF));
        buf.put((byte)(addr >> 16 & 0xFF));
        buf.put((byte)(addr >> 8 & 0xFF));
        buf.put((byte)(addr >> 0 & 0xFF));

        this.addr = InetAddress.getByAddress(bytes);
    }

    /**
     * Get IPV6Address as an InetAddress
     */
    public InetAddress asInetAddress() {
        return addr;
    }

    /**
     * Get IPV6Address as an Integer.
     */
    public int asInteger() {
        throw new UnsupportedOperationException("IPV6Address does not support asInteger()");
    }

    /**
     * Address in transmittable form
     */
    public String asTransmitForm() {
        return numericToTextFormat(bytes);
    }

    /**
     * Compare this Address to another one
     */
    public int compareTo(Object other) {
        throw new UnsupportedOperationException("IPV6Address does not support compareTo");
    }

    /**
     * Equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof Address) {
            Address addr = (Address)obj;
            byte[] me = this.asByteArray();
            byte[] other = addr.asByteArray();

            for (int i = 0; i<16; i++) {
                if (me[i] != other[i]) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * hash code
     */
    public int hashCode() {
        // convert byte [] to int
        // get 4 ints and add them up
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        int result = 0;

        int i1 = buf.getInt();
        int i2 = buf.getInt();
        int i3 = buf.getInt();
        int i4 = buf.getInt();

        int sum = i1+ i2 + i3 + i4;

        return sum;
    }

    /**
     * To String
     */
    public String toString() {
        return numericToTextFormat(bytes);
    }

}