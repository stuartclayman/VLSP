package usr.net;

import java.nio.ByteBuffer;
import usr.logging.*;

/**
 * A Datagram.
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
