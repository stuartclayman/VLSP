package usr.net;

import java.nio.ByteBuffer;
import usr.logging.*;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;

/**
 * An GID Address
 */
public class GIDAddress extends Size4 implements Address, Serializable {
    int globalAddress;

    /**
     * Create a GIDAddress from a String
     */
    public GIDAddress(String gidStr)  throws UnknownHostException {
        Scanner scanner = new Scanner(gidStr);

        if (scanner.hasNextInt()) {
            int gid = scanner.nextInt();

            globalAddress = gid;

            // convert int to byte[] 
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.putInt(gid);


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
        return "@("+globalAddress + ")";
    }

    /**
     * Convert.
     */
    static String numericToTextFormat(byte[] src)
    {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
    }


}

