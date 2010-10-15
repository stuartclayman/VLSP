package usr.net;

import java.net.InetAddress;
import usr.logging.*;

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
     * Get Address as an Integer
     */
    public int asInteger();

    /**
     * Get an Address as an InetAddress
     */
    public InetAddress asInetAddress();
} 
