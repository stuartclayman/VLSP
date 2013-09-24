package usr.net;

import java.net.SocketException;

public class SocketTimeoutException extends SocketException {

    public SocketTimeoutException() {
        super();
    }

    public SocketTimeoutException(String msg) {
        super(msg);
    }


}
