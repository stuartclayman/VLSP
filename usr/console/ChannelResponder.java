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
	//System.err.println("Responding");
	try {
	    return writeBytesToChannel(ByteBuffer.wrap(message.getBytes()));
	} catch (IOException ioe) {
	    Logger.getLogger("log").logln(USR.ERROR,
	                                  "Channel responder found an error writing to socket channel");
	    Logger.getLogger("log").logln(USR.ERROR,"Channel responder message was "+ioe.getMessage());
	    return false;
	}
    }

    public boolean writeBytesToChannel(ByteBuffer bb) throws IOException {
	int len= channel.write(bb);
	if (len < 0)
	    return false;
	while (bb.remaining()>0) {
	    /*
	       // Here to be cautious
	       try {
	        Thread.sleep(10);
	       } catch (InterruptedException e) {
	       }
	     */


	    len= channel.write(bb);
	    if (len < 0) {
		return false;
	    }
	}
	return true;
    }

}
