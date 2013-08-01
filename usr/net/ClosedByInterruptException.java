package usr.net;

import java.net.SocketException;

public class ClosedByInterruptException extends SocketException {

    public ClosedByInterruptException() {
        super();
    }

    public ClosedByInterruptException(String msg) {
        super(msg);
    }


}
