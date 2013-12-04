package usr.net;

import java.net.SocketException;

public class ClosedByInterruptException extends SocketException {

    /**
     * 
     */
    private static final long serialVersionUID = -724759872977290821L;

    public ClosedByInterruptException() {
        super();
    }

    public ClosedByInterruptException(String msg) {
        super(msg);
    }


}
