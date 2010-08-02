package usr.net;

import java.nio.ByteBuffer;

/**
 * A Datagram.
 */
interface DatagramPatch {
    /**
     * To ByteBuffer.
     */
    public ByteBuffer toByteBuffer();

    /**
     * From ByteBuffer.
     */
    public boolean fromByteBuffer(ByteBuffer b);
}
