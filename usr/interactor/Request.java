package usr.interactor;

import java.nio.channels.SocketChannel;

/**
 * A request to a Router.
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
