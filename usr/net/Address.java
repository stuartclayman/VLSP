package usr.net;

public interface Address {
    /**
     * Get the size in bytes of an instantiation of an Address.
     */
    public int size();

    /**
     * Get Address as a byte[]
     */
    public byte[] asByteArray();
} 
