package usr.net;

/**
 * For addresses that are 4 bytes long.
 */
abstract class Size4 implements Address {
    // the bytes for the address
    protected byte[] bytes = new byte[4];

    // an EMPTY address
    public final static byte[] EMPTY = { 0, 0, 0, 0 };



}
