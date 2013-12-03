package usr.net;

import java.net.SocketException;

public class SocketTimeoutException extends SocketException {

    /**
     *
     */
    private static final long serialVersionUID = 1929904056426428046L;

    public SocketTimeoutException() {
        super();
    }

    public SocketTimeoutException(String msg) {
        super(msg);
    }

}