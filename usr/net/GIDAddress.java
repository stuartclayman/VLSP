package usr.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * An GID Address
 */
public class GIDAddress extends Size4 implements Address, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 6415450506543742461L;
    int globalAddress;

    /**
     * Create a GIDAddress from a String
     */
    public GIDAddress(String gidStr)  throws UnknownHostException {
        Scanner scanner = new Scanner(gidStr);

        if (scanner.hasNextInt()) {
            int gid = scanner.nextInt();
            scanner.close();
            globalAddress = gid;

            // convert int to byte[]
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.putInt(gid);


        } else {
            scanner.close();
            throw new UnknownHostException("Not a GID: " + gidStr);
        }
    }

    /**
     * Create a GIDAddress from an int
     */
    public GIDAddress(int addr) {
        globalAddress = addr;
        // convert int to byte[]
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putInt(addr);

    }

    /**
     * Create a GIDAddress from a byte[]
     */
    public GIDAddress(byte[] addr)  throws UnsupportedOperationException {
        if (addr.length == 4) {
            // copy bytes in
            System.arraycopy(addr, 0, bytes, 0, 4);

            // convert byte[] to int
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            globalAddress = buf.getInt();

        } else {
            throw new UnsupportedOperationException("GIDAddress: wrong length. Expected 4, got " + addr.length);
        }
    }

    /**
     * Get GIDAddress as an Integer.
     */
    @Override
    public int asInteger() {
        return globalAddress;
    }

    /**
     * Get GIDAddress as an InetAddress
     */
    @Override
    public InetAddress asInetAddress() {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

    /**
     * Address in transmittable form
     */
    @Override
    public String asTransmitForm() {
        return Integer.toString(globalAddress);
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
        return "@("+globalAddress + ")";
    }

}
