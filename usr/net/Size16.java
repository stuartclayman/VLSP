package usr.net;


/**
 * For addresses that are 16 bytes long.
 */
public abstract class Size16 implements Address {
    // the bytes for the address
    protected byte[] bytes = new byte[16];

    // an EMPTY address
    public final static byte[] EMPTY = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    /**
     * Get the size in bytes of an instantiation of an Size4 Address.
     */
    @Override
	public int size() {
        return 16;
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
    static String numericToTextFormat(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        // 1080:0:0:0:8:800:200C:417A

        builder.append(format(bytes, 0, 1));
        builder.append(":");
        builder.append(format(bytes, 2, 3));
        builder.append(":");
        builder.append(format(bytes, 4, 5));
        builder.append(":");
        builder.append(format(bytes, 6, 7));
        builder.append(":");
        builder.append(format(bytes, 8, 9));
        builder.append(":");
        builder.append(format(bytes, 10, 11));
        builder.append(":");
        builder.append(format(bytes, 12, 13));
        builder.append(":");
        builder.append(format(bytes, 14, 15));

        return builder.toString();
    }

    static private String format(byte[] bb, int fst, int snd) {
        byte a = bb[fst];
        byte b = bb[snd];

        if (a == 0) {
            if (b == 0) {
                return "0";
            } else {
                return Integer.toHexString(b & 0xFF);
            }
        } else {
            return Integer.toHexString(a & 0xFF) + Integer.toHexString(b & 0xFF);
        }
    }

    @Override
	public int hashCode() {
        return asInteger();
    }

}