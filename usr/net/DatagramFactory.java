package usr.net;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.Protocol;

/**
 * The DatagramFactory will create a new datagram based on
 * the protocol number.
 * For example, if the Protocol is DATA, and the class set for
 * the Protocol is usr.net.GIDDatagram, it will return
 * an GIDDatagram.
 */
public class DatagramFactory {
    // The list of Protocols to DatagramFactoryInfo objects
    // indexed by Protocol number
    private static ArrayList<DatagramFactoryInfo> list = new ArrayList<DatagramFactoryInfo>();

    /** Set the default TTL to be used by new packets */
    static int initialTTL_ = 64;

    // class initiation code
    static {
        setClassForProtocol("usr.net.Size4Datagram", Protocol.DATA);
        setClassForProtocol("usr.net.Size4Datagram", Protocol.CONTROL);
    }

    /**
     * Return an empty Datagram.
     */
    static Datagram newDatagram() {
        return newDatagram(Protocol.DATA, (ByteBuffer)null);
    }

    /**
     * Return a Datagram given a ByteBuffer
     */
    public static Datagram newDatagram(ByteBuffer payload) {
        return newDatagram(Protocol.DATA, payload);
    }

    /**
     * Return a Datagram given a byte[]
     */
    public static Datagram newDatagram(byte[] payload) {
        return newDatagram(Protocol.DATA, ByteBuffer.wrap(payload));
    }

    /**
     * Return the relevant Datagram given a ByteBuffer
     */
    public static Datagram newDatagram(int protocol, ByteBuffer payload) {
        DatagramFactoryInfo dfi = null;

        // now allocate object
        try {
            // get DatagramFactoryInfo for protocol
            dfi = list.get(protocol);

            if (payload == null) {
                Datagram dg = dfi.cons0.newInstance();
                return dg;
            } else {
                Datagram dg = dfi.cons1.newInstance(payload);
                dg.setTTL(initialTTL_);
                dg.setProtocol(protocol);
                return dg;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, "DatagramFactory: Exception: " + e);
            throw new Error("DatagramFactory: config error in DatagramFactory.  Cannot allocate an instance of: " + (dfi == null ? "null" : dfi.className) + " for protocol " + protocol);
        }

    }

    /**
     * Return the relevant Datagram given a ByteBuffer
     */
    public static Datagram newDatagram(int protocol, byte[] payload) {
        return newDatagram(protocol, ByteBuffer.wrap(payload));
    }


    /**
     * Copy a Datagram
     */
    public static Datagram copy(Datagram dg) {
        int protocol = dg.getProtocol();

        DatagramFactoryInfo dfi = null;

        try {
            // get DatagramFactoryInfo for protocol
            dfi = list.get(protocol);

            // get the array of the Datagram
            byte[] array = ((DatagramPatch)dg).toByteBuffer().array();
            int capacity = array.length;

            // copy stuff in to new array
            byte[] newArray = new byte[capacity];
            System.arraycopy(array, 0, newArray, 0, capacity);


            // create a new ByteBuffer from the new array
            Datagram newDG = dfi.cons0.newInstance();
            ByteBuffer newBuf = ByteBuffer.wrap(newArray);

            ((DatagramPatch)newDG).fromByteBuffer(newBuf);

            return newDG;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, "DatagramFactory: Exception: " + e);
            throw new Error("DatagramFactory: config error in DatagramFactory.  Cannot allocate an instance of: " + (dfi == null ? "null" : dfi.className) + " for protocol " + protocol);
        }

        
    }


    /**
     * Return the Constructor for creating a Datagram with a ByteBuffer,
     * for a given protocol.
     */
    public static Constructor<? extends Datagram> getByteBufferConstructor(int protocol) {
        // get DatagramFactoryInfo for protocol
        DatagramFactoryInfo dfi = list.get(protocol);

        return dfi.cons1;
    }

    /**
     * Set up the class for a protocol
     */
    public static void setClassForProtocol(String className, int protocol) {
        //System.err.println("DatagramFactory: setClassForProtocol " + className + " " + protocol);

        DatagramFactoryInfo dfi = new DatagramFactoryInfo(className);

        if (list.size() <= protocol || list.get(protocol) == null) {
            list.add(protocol, dfi);
        } else {
            list.set(protocol, dfi);
        }
    }

    /** Set the default Initial TTL for each datagram type */
    public static void setInitialTTL(int ttl) {
        initialTTL_ = ttl;
    }

}

class DatagramFactoryInfo {
    public String className;
    public Constructor<? extends Datagram> cons0;
    public Constructor<? extends Datagram> cons1;

    public DatagramFactoryInfo(String name) {
        try {
            className = name;

            // get Class object
            Class<?> c = Class.forName(className);

            final Class<? extends Datagram> xc = c.asSubclass(Datagram.class );
            // find Constructor for when arg is null
            cons0 = xc.getDeclaredConstructor();


            // get Consturctor for when arg is ByteBuffer
            cons1 = xc.getDeclaredConstructor(ByteBuffer.class );

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "DatagramFactoryInfo: Exception: " + e);
            throw new Error("DatagramFactory: config error in DatagramFactory.  Cannot configure class data for: " + className);
        }
    }

}
