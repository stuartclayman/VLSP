// BitMask.java

package usr.logging;

/**
 * A BitMask object holds a bit mask.  It has methods to
 * get and set bits.
 * It is similar to java.util.BitSet, but is less cumbersome.
 * On the other hand it can only be 32 bits.
 */
public class BitMask implements Cloneable {
    // a mask can be 32 bits
    int actual;

    /**
     * Construct a new BitMask with value of 0
     */
    public BitMask() {
        actual = 0;
    }

    /**
     * Construct a BitMask from an integer
     */
    public BitMask(int m) {
        actual = m;
    }

    /**
     * Performs a logical AND of this mask with the argument mask.
     * A new mask is returned so that each bit in it has the value
     * true if and only if it both initially had the value true and the
     * corresponding bit in the mask argument also had the value true.
     */
    public BitMask and(BitMask mask) {
        return new BitMask(this.actual & mask.actual);
    }

    /**
     * Performs a logical OR of this mask with the argument mask.
     * A new mask is returned so that a bit in it has the value
     * true if and only if it either already had the value true or the
     * corresponding bit in the mask argument has the value true.
     */
    public BitMask or(BitMask mask) {
        return new BitMask(this.actual | mask.actual);
    }

    /**
     * Performs a logical XOR of this mask with the argument mask.
     * A new mask is returned so that a bit in it has the value
     * true if and only if one of the following statements holds: The bit
     * initially has the value true, and the corresponding bit in the
     * argument has the value false.  The bit initially has the value false,
     * and the corresponding bit in the argument has the value true.
     */
    public BitMask xor(BitMask mask) {
        return new BitMask(this.actual ^ mask.actual);
    }

    /**
     * Sets the bit at the specified index.
     * Returns this BitMask.
     */
    public BitMask set(int bitIndex) {
        actual |= (1 << bitIndex);
        return this;
    }

    /**
     * Unsets the bit at the specified index.
     * Returns this BitMask.
     */
    public BitMask unset(int bitIndex) {
        actual &= (1 << bitIndex);
        return this;
    }

    /**
     * Is the bit at the specified index set?
     * Returns true if it is, false otherwise.
     */
    public boolean isSet(int bitIndex) {
        if ((actual & (1 << bitIndex)) == 0) {
            // bit not set
            return false;
        } else {
            return true;
        }
    }

    /**
     * Sets all of the bits to 0
     * Returns this BitMask.
     */
    public BitMask clear() {
        actual = 0;
        return this;
    }

    /**
     * Is the mask clear?
     * That is, is any bit set?
     * Returns true if it is, false otherwise.
     */
    public boolean isClear() {
        return (actual == 0);
    }

    /**
     * Inverts all of the bits of this BitMask.
     * Returns this BitMask.
     */
    public BitMask invert() {
        actual = ~actual;
        return this;
    }

    /**
     * Cloning a BitMask produces a new BitMask that is equal to it.
     * The clone of the bit set is another bit set that has exactly
     * the same bits set to true as this mask.
     */
    @Override
	public Object clone() {
        return new BitMask(actual);
    }

    /**
     * Equals
     */
    @Override
	public boolean equals(Object obj) {
        if (obj instanceof BitMask) {
            if (actual == ((BitMask)obj).actual) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Hash Code.
     */
    @Override
	public int hashCode() {
        // actual is unique enough
        return actual;
    }

    /**
     * To String
     */
    @Override
	public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int b = 31; b>=0; b--) {
            if ((actual & (1 << b)) == 0) {
                // bit is not set
                builder.append('0');
            } else {
                builder.append('1');
            }
        }

        return builder.toString();

    }

}