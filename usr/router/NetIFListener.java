package usr.router;
import java.net.NoRouteToHostException;

import usr.net.Address;
import usr.net.Datagram;
/**
 * Interface is for "glue" to hold together netifs -- it allows routing between them
 */
public interface NetIFListener extends RouterFabric {


    /** Return the router Fabric device for this datagram -- this is
       the correct way to route datagrams */
    public FabricDevice lookupRoutingFabricDevice(Datagram dg) throws NoRouteToHostException;

    /** Is this address an address associated with this netiflistener*/
    public boolean ourAddress(Address a);

    /** Deal with TTL expire */
    public void TTLDrop(Datagram dg);

    /** A datagram device has closed and must be removed */
    void closedDevice(DatagramDevice dd);

}
