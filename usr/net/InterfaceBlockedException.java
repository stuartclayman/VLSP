package usr.net;

public class InterfaceBlockedException extends Exception {

    public InterfaceBlockedException() {
        super();
    }

    public InterfaceBlockedException(String s) {
        super(s);
    }

    /**
     * 
     */
    private static final long serialVersionUID = -2165082078871728369L;

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
