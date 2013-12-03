package usr.net;

import usr.logging.*;
import java.lang.reflect.Constructor;
import usr.router.Router;

/**
 * The AddressFactory will create a new Address object based on
 * the class set for Addresses.
 * For example, if Addresses are set to use usr.net.GIDAddress,
 * then it will return a GIDAddress.
 */
public class AddressFactory {
    // The Constructor for Addresses passed by int
    static Constructor<? extends Address> consI;
    // The Constructor for Addresses passed by byte[]
    static Constructor<? extends Address> consB;
    // The Constructor for Addresses passed by String
    static Constructor<? extends Address> consS;

    // Name of  class
    static String className = null;

    // class initiation code
    static {
        setClassForAddress("usr.net.GIDAddress");
        //setClassForAddress("usr.net.DomainAddress");
        //setClassForAddress("usr.test.IPV6Address");
    }

    /**
     * Return an Address, given an int
     */
    public static Address newAddress(int addr) throws java.net.UnknownHostException {
        Exception finalE = null;
        try {
            Address address = consI.newInstance(addr);
            return address;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable e2 = e.getTargetException();

            if (e2 instanceof java.net.UnknownHostException) {
                throw (java.net.UnknownHostException)e2;
            }
            Logger.getLogger("log").logln(USR.ERROR, "Unknown Host " + (e2.getClass()));
            finalE = new Exception(e.getMessage());
        } catch (Exception e) {
            finalE = e;
        }

        finalE.printStackTrace();
        Logger.getLogger("log").logln(USR.ERROR, "AddressFactory: Exception: " + finalE);
        throw new Error("AddressFactory: config error in AddressFactory.  Cannot allocate an instance of: " + className);
    }

    /**
     * Return an Address, given a byte[]
     */
    public static Address newAddress(byte[] addr) {
        try {
            Address address = consB.newInstance(addr);
            return address;
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "AddressFactory: Exception: " + e);
            throw new Error("AddressFactory: config error in AddressFactory.  Cannot allocate an instance of: " + className);

        }
    }

    /**
     * Return an Address, given a String
     */
    public static Address newAddress(String addr) throws java.net.UnknownHostException  {
        Exception finalE = null;
        try {
            Address address = consS.newInstance(addr);
            return address;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable e2 = e.getTargetException();

            if (e2 instanceof java.net.UnknownHostException) {
                throw (java.net.UnknownHostException)e2;
            }
            System.err.println (e2.getClass());
            finalE = new Exception(e.getMessage());
        } catch (Exception e) {
            finalE = e;
        }

        finalE.printStackTrace();
        Logger.getLogger("log").logln(USR.ERROR, "AddressFactory: Exception: " + finalE);
        throw new Error("AddressFactory: config error in AddressFactory.  Cannot allocate an instance of: " + className);

    }

    /**
     * Get the current class for an Address.
     */
    public static String getClassForAddress() {
        return className;
    }

    /**
     * Set up the class for an Address
     */
    public static void setClassForAddress(String name) {
        try {
            className = name;

            //System.err.println("AddressFactory: setClassForAddress " + className);

            // get Class object
            Class<?> c = Class.forName(className);

            // get it as more exact type
            Class<? extends Address> xc = c.asSubclass(Address.class );

            // find Constructor for when arg is int
            consI = xc.getDeclaredConstructor(int.class );


            // get Consturctor for when arg is byte[]
            consB = xc.getDeclaredConstructor(byte[].class );

            // get Consturctor for when arg is String
            consS = xc.getDeclaredConstructor(String.class );

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "AddressFactory: Exception: " + e);
            e.printStackTrace();
            throw new Error("AddressFactory: config error in AddressFactory.  Cannot configure class data for: " + className);
        }
    }

    /**
     * Get address of current router
     */
    public static Address getAddress() {
        Router r = usr.router.RouterDirectory.getRouter();

        if (r == null) {
            return null;
        } else {
            return r.getAddress();
        }
    }

}