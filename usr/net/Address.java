package usr.net;

import java.net.InetAddress;

public interface Address extends Comparable <Object> {
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

    /**
     * Get an Address in a transmittable form
     * which can be reconstructed directed from this format.
     */
    public String asTransmitForm();
}