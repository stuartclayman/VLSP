package usr.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * An IPV4 Address
 */
public class IPV4Address extends Size4 implements Address, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 2090554822014106211L;
    InetAddress addr;

    /**
     * Create a IPV4Address from a hostname
     */
    public IPV4Address(String hostname) throws UnknownHostException {
        addr = InetAddress.getByName(hostname);
        byte[] inetbytes = addr.getAddress();
        // copy bytes in
        //System.arraycopy(inetbytes, 0, bytes, 0, 4);
        bytes[0] = inetbytes[0];
        bytes[1] = inetbytes[1];
        bytes[2] = inetbytes[2];
        bytes[3] = inetbytes[3];

    }

    /**
     * Create a IPV4Address from a byte[]
     */
    public IPV4Address(byte[] addr)  throws UnknownHostException {
        if (addr.length == 4) {
            // copy bytes in
            //System.arraycopy(addr, 0, bytes, 0, 4);
            bytes[0] = addr[0];
            bytes[1] = addr[1];
            bytes[2] = addr[2];
            bytes[3] = addr[3];

            this.addr = InetAddress.getByAddress(bytes);
        } else {
            throw new UnknownHostException("InetAddress: wrong length. Expected 4, got " + addr.length);
        }
    }

    /**
     * Create an IPV4Address from an int
     */
    public IPV4Address(int addr) throws UnknownHostException {
        // convert int to byte[]
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putInt(addr);
        this.addr = InetAddress.getByAddress(bytes);
    }

    /**
     * Get IPV4Address as an InetAddress
     */
    @Override
    public InetAddress asInetAddress() {
        return addr;
    }

    /**
     * Get IPV4Address as an Integer.
     */
    @Override
    public int asInteger() {
        // convert byte[] to int
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return buf.getInt();
    }

    /**
     * Address in transmittable form
     */
    @Override
    public String asTransmitForm() {
        return numericToTextFormat(bytes);
    }

    /**
     * Compare this Address to another one
     */
    @Override
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
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Address) {
            Address addr = (Address)obj;
            return addr.asInteger() == this.asInteger();
        } else {
            return false;
        }
    }

    /**
     * hashcode
     */
    @Override
    public int hashCode() {
        return asTransmitForm().hashCode();
    }

    /**
     * To String
     */
    @Override
    public String toString() {
        return numericToTextFormat(bytes);
    }

}
