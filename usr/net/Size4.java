package usr.net;

/**
 * For addresses that are 4 bytes long.
 */
public abstract class Size4 implements Address {
    // the bytes for the address
    protected byte[] bytes = new byte[4];

    // an EMPTY address
    public final static byte[] EMPTY = { 0, 0, 0, 0 };

    /**
     * Get the size in bytes of an instantiation of an Size4 Address.
     */
    @Override
	public int size() {
        return 4;
    }

    /**
     * Get Size4 Address as a byte[]
     */
    @Override
	public byte[] asByteArray() {
        return bytes;
    }

    /**
     * Convert.
     */
    static String numericToTextFormat(byte[] src) {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
    }

    @Override
	public int hashCode() {
        return asInteger();
    }

}