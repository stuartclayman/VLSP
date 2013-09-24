package usr.net;

import usr.logging.*;
import java.util.ArrayList;


public class DatagramPacket extends Size4Datagram {
    public DatagramPacket(byte[] payload) {
        super(payload);
    }
    
    public DatagramPacket(byte[] payload, int length) {
        super(payload);
    }
    

    public DatagramPacket(byte[] payload, Address addr, int port) {
        super(payload, addr, port);
    }
    
}
