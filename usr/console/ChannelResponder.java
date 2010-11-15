package usr.console;

import java.nio.ByteBuffer;
import usr.logging.*;
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
            int count= channel.write(ByteBuffer.wrap(message.getBytes()));
            if (count != message.getBytes().length) {
                System.err.println("YIPE MESSAGE LEN ISSUE");
            }
            return true;
        } catch (IOException ioe) {
              Logger.getLogger("log").logln(USR.ERROR, 
              "Channel responder found an error writign to socket channel "+ioe.getMessage());
            return false;
        }
    }

}
