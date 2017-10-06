package usr.router;
import java.net.NoRouteToHostException;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.Datagram;

/**
 * A Minimal version of datagram device -- given an address and a fabric device
   it routes packets either to the fabric device or receives them as appropriate
   according to the Listener device -- to instantiate implement outQueueHandler
 */
public abstract class MinimalDatagramDevice implements DatagramDevice {
    Address address_ = null;
    FabricDevice fabric_ = null;
    NetIFListener listener_ = null;
    String name_;


    /** Create a very basic datagram device -- this is not properly instatiated
       until address and listener set*/
    public MinimalDatagramDevice(String name) {
        name_ = new String(name);
    }

    /** Given an address and a netListener create a minimal datagram device
       with a very basic datagram device*/
    public MinimalDatagramDevice(String name, Address addr, NetIFListener l) {
        this(name, addr, null, l);
    }

    /** Given an address and a netListener and a fabric create a minimal datagram device */
    public MinimalDatagramDevice(String name, Address addr, FabricDevice fd, NetIFListener l) {
        name_ = name;
        address_ = addr;
        fabric_ = fd;
        listener_ = l;
        name_ = new String("MinimalDatagramDevice");

    }

    /**
       Initialise must be called if fabric device not set up in constructor
     */
    public void start() {
        if (fabric_ != null) {
            fabric_.start();
            return;
        }

        if (listener_ == null) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          "Minimal Datagram Device needs listener to create fabric device");
        }
        fabric_ = new FabricDevice(this, listener_);
        fabric_.setName(name_);
        fabric_.start();

    }

    /**
     * Get the name of this Net Device
     */
    @Override
    public String getName() {
        return name_;
    }

    /** is this address ours */
    public boolean ourAddress(Address addr) {
        if (addr == null) {
            return (address_ == null);
        }
        return (addr.equals(address_));
    }

    /**
     * Set the name of this Net Device
     */
    @Override
    public void setName(String name) {
        name_ = name;

        if (fabric_!= null) {
            fabric_.setName(name);
        }
    }

    /**
     * Get the Address for this connection.
     */
    @Override
    public Address getAddress() {
        return address_;
    }

    /** Get the FabricDevice associated with Net Device */

    @Override
    public FabricDevice getFabricDevice() {
        return fabric_;
    }

    /**
     * Set the Address for this connection.
     */
    @Override
    public void setAddress(Address addr) {
        address_ = addr;
    }

    /**
     * Send a Datagram originating at this host (sets src address) and
     */
    @Override
    public boolean sendDatagram(Datagram dg) throws NoRouteToHostException {
        return enqueueDatagram(dg);
    }

    /**
     * forward a datagram (does not set src address)
     */
    @Override
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException {
        FabricDevice fd = listener_.lookupRoutingFabricDevice(dg);
        return fd.addToInQueue(dg, null);
    }

    public void stop() {
        if (fabric_ != null) {
            fabric_.stop();
        }
    }

    /**
     *   Send the datagram onwards to the world
     */
    @Override
    public abstract boolean recvDatagramFromDevice(Datagram dg, DatagramDevice dd);

    /**
     * Get the Listener of a NetIF.
     */
    @Override
    public NetIFListener getNetIFListener() {
        return listener_;
    }

    /**
     * Set the Listener of NetIF.
     */
    @Override
    public void setNetIFListener(NetIFListener l) {
        listener_ = l;

    }

}
