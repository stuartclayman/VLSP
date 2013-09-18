package usr.net;

public class InterfaceBlockedException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2165082078871728369L;

	@Override
	public Throwable fillInStackTrace() {
        return this;
    }

}