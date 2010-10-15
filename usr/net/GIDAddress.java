package usr.net;

import java.nio.ByteBuffer;
import usr.logging.*;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * An GID Address
 */
public class GIDAddress extends Size4 implements Address {
    int globalAddress;

    /**
     * Create a GIDAddress from a String
     */
    public GIDAddress(String gidStr)  throws UnknownHostException {
        Scanner scanner = new Scanner(gidStr);

        if (scanner.hasNextInt()) {
            int gid = scanner.nextInt();

            globalAddress = gid;
        } else {
            throw new UnknownHostException("Not a GID: " + gidStr);
        }
    }

    /**
     * Create a GIDAddress from an int
     */
    public GIDAddress(int addr) {
        globalAddress = addr;
        //Logger.getLogger("log").logln(USR.STDOUT, "SET GLOBAL ADDRESS "+addr);

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
     * Get GIDAddress as an Integer.
     */
    public int asInteger() {
        return globalAddress;
    }


    /**
     * Get the size in bytes of an instantiation of an GIDAddress.
     */
    public int size() {
        return 4;
    }

    /**
     * Get GIDAddress as an InetAddress
     */
    public InetAddress asInetAddress() {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException uhe) {
            return null;
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
        return "@("+globalAddress + ")";
    }

}

