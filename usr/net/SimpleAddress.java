package usr.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A very simple address system with just a flat label string
 */
public class SimpleAddress implements Address {
    private String address_= null;
    
    public SimpleAddress (String s) {
        address_= s;
    }
    
    
    public byte [] asByteArray() {
        return address_.getBytes();
    }
    
    public int size () {
        return asByteArray().length;
    }
    
    public InetAddress asInetAddress() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    
}

