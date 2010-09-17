package usr.net;

import usr.protocol.Protocol;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.lang.reflect.Constructor;

/**
 * The DatagramFactory will create a new datagram based on 
 * the protocol number.
 * For example, if the Protocol is IPV4_DATA, it will return
 * an IPV4Datagram.
 */
public class DatagramFactory {
    // The list of Protocols to DatagramFactoryInfo objects
    // indexed by Protocol number
    private static ArrayList<DatagramFactoryInfo> list = new ArrayList<DatagramFactoryInfo>();

    // class initiation code
    static {
        setClassForProtocol("usr.net.IPV4Datagram", Protocol.DATA);
        setClassForProtocol("usr.net.IPV4Datagram", Protocol.CONTROL);
    }


    /**
     * Return the relevant Datagram.
     */
    public static Datagram newDatagram(int protocol, ByteBuffer payload) {
        /*
        if (payload == null) {
            return new IPV4Datagram();
        } else {
            return new IPV4Datagram(payload);
        }
        */

        // get DatagramFactoryInfo for protocol
        DatagramFactoryInfo dfi = list.get(protocol);

        // now allocate object
        try {
            if (payload == null) {
                return (Datagram)dfi.cons0.newInstance();
            } else {
                return (Datagram)dfi.cons1.newInstance(payload);            
            }
        } catch (Exception e) {
            throw new Error("DatagramFactory: config error in DatagramFactory.  Cannot allocate an instance of: " + dfi.className);
        }

    }


    /**
     * Set up the class for a protocol
     */
    public static void setClassForProtocol(String className, int protocol) {
        DatagramFactoryInfo dfi = new DatagramFactoryInfo(className);

        if (list.size() <= protocol || list.get(protocol) == null) {
            list.add(protocol, dfi);
        } else {
            list.set(protocol, dfi);
        }
    }

        
}

class DatagramFactoryInfo {
    public String className;
    public Class clazz;
    public Constructor<Datagram> cons0;
    public Constructor<Datagram> cons1;

    public DatagramFactoryInfo(String name) {
        try {
            className = name;

            // get Class object
            clazz = Class.forName(className);

            // find Constructor for when arg is null
            cons0 = (Constructor<Datagram>)clazz.getConstructor();


            // get Consturctor for when arg is ByteBuffer
            cons1 = (Constructor<Datagram>)clazz.getConstructor(ByteBuffer.class);

        } catch (Exception e) {
            System.err.println("DatagramFactoryInfo: Exception: " + e);
            throw new Error("DatagramFactory: config error in DatagramFactory.  Cannot configure class data for: " + className);
        }
    }
}
