package usr.net;

import java.nio.ByteBuffer;
import java.net.InetAddress;

/**
 * An GID Address
 */
public class GIDAddress implements Address {
    int globalAddress;
    byte[] bytes = new byte[4];

    // an EMPTY address
    public final static byte[] EMPTY = { 0, 0, 0, 0};


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
            bytes = addr;

            // convert byte[] to int
            ByteBuffer buf = ByteBuffer.wrap(addr);
            globalAddress = buf.getInt();

        } else {
            throw new UnsupportedOperationException("GIDAddress: wrong length. Expected 4, got " + addr.length);
        }
    }

    /**
     * Get GIDAddress as a byte[]
     */
    public byte[] asByteArray() {
        return bytes;
    }

    /**
     * Get the size in bytes of an instantiation of an GIDAddress.
     */
    public int size() {
        return 4;
    }

    /**
     * Get GIDAddress as an InetAddress
     * @throws UnsupportedOperationException always
     */
    public InetAddress asInetAddress() {
        throw new UnsupportedOperationException("GIDAddress: does not support InetAddress");
    }

    /**
     * To String
     */
    public String toString() {
        return "@" + globalAddress;
    }

}

