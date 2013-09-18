package usr.net;

import java.nio.ByteBuffer;

/**
 * An interface for Datagram obejcts that can patch themselves up to and from a ByteBuffer.
 */
public interface DatagramPatch {
    /**
     * To ByteBuffer.
     */
    public ByteBuffer toByteBuffer();

    /**
     * From ByteBuffer.
     */
    public boolean fromByteBuffer(ByteBuffer b);
}