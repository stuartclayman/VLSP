package usr.net;

public class InterfaceBlockedException extends Exception {

    public Throwable fillInStackTrace() {
        return this;
    }

}