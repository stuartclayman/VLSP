package usr.router;
import usr.net.Datagram;
import usr.net.Address;
import java.net.NoRouteToHostException;

import usr.logging.*;
/**
 * Interface is for "glue" to hold together netifs -- it allows routing between them
 */
public interface NetIFListener {


    /** Return the router Fabric device for this datagram -- this is
       the correct way to route datagrams */
    public FabricDevice getRouteFabric(Datagram dg) throws NoRouteToHostException;

    /** Is this address an address associated with this netiflistener*/
    public boolean ourAddress(Address a);

    /** Deal with TTL expire */
    public void TTLDrop(Datagram dg);

    /** A datagram device has closed and must be removed */
    void closedDevice(DatagramDevice dd);

    /**
     * Get it's name
     */
    public String getName();
}