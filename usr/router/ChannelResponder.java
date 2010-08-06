package usr.router;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.IOException;

/**
 * A ChannelResponder is a class that can respond down
 * a Channel with success and error codes.
 */
public abstract class ChannelResponder {
    // The SocketChannel
    SocketChannel channel;

    /**
     * Get the SocketChannel this command 
     * is a handler for.
     */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
     * Set the SocketChannel this command 
     */
    public void setChannel(SocketChannel ch) {
        channel = ch;
    }

    /**
     * Respond with a given string.
     * Returns false if it cannot send the response down the channel.
     */
    public boolean respond(String message) {
        message = message.concat("\n");

        try {
            channel.write(ByteBuffer.wrap(message.getBytes()));
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

}
