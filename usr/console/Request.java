package usr.console;

import java.nio.channels.SocketChannel;
import usr.logging.*;

/**
 * A request to a Component.
 */
public class Request {
    public final SocketChannel channel;
    public final String value;

    public Request(SocketChannel ch, String str) {
	channel = ch;
	value = str;
    }

    public String toString() {
	return value + " @ " + channel;
    }
}
