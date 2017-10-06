package usr.dcap;

import usr.router.Router;
import usr.router.RouterDirectory;
import usr.router.NetIF;
import usr.router.Intercept;
import usr.net.Address;
import usr.net.SocketTimeoutException;
import java.net.SocketException;
import java.util.List;
import java.util.ArrayList;

public class DcapNetworkInterface {

    NetIF netIF;

    /**
     * Construct a NetworkInterface
     */
    DcapNetworkInterface(NetIF netIF) {
        this.netIF = netIF;
    }


    /**
     * Open a NetworkInterface, allows upto 65536 bytes of a Datagram to be captured.
     * All Datagrams are copied up to the Dcap.
     */
    public Dcap open() {
        return new Dcap(this, 65536);
    }

    /**
     * Open a NetworkInterface, with a snapLen which limits the no of bytes captures for a Datagram
     */
    public Dcap open(int snapLen) {
        return new Dcap(this, snapLen);
    }

    /**
     * Intercept a NetworkInterface, allows upto 65536 bytes of a Datagram to be captured.
     * All Datagrams are sent to the Intercept and NOT routed.
     */
    public Intercept intercept() {
        return new Intercept(this.netIF, 65536);
    }

    /**
     * Get the interface by address.
     */
    public static DcapNetworkInterface getIFByAddress(Address addr) throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        // Get the NetIFs from the fabric
        List<NetIF> netIFs = router.getRouterFabric().listNetIF();

        DcapNetworkInterface result = null;

        // Build list of NetworkInterfaces
        for (NetIF netif : netIFs) {
            System.out.println("netif = " + netif);

            if (netif.getAddress().equals(addr) || netif.getRemoteRouterAddress().equals(addr)) {
                // found it
                result = new DcapNetworkInterface(netif);
            }
        }

        return result;
    }

    /**
     * Get the interface by name.
     */
    public static DcapNetworkInterface getIFByName(String name)  throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        // Get the NetIFs from the fabric
        List<NetIF> netIFs = router.getRouterFabric().listNetIF();

        DcapNetworkInterface result = null;

        // Build list of NetworkInterfaces
        for (NetIF netif : netIFs) {
            if (netif.getName().equals(name)) {
                // found it
                result = new DcapNetworkInterface(netif);
            }
        }

        return result;
    }

    /**
     * Get the interface by index == the router port number.
     */
    public static DcapNetworkInterface getIFByIndex(int index)  throws SocketException  {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        // Get the NetIFs from the fabric
        List<NetIF> netIFs = router.getRouterFabric().listNetIF();

        DcapNetworkInterface result = null;

        // Build list of NetworkInterfaces
        for (NetIF netif : netIFs) {
            if (netif.getRouterPort().getPortNo() == index) {
                // found it
                result = new DcapNetworkInterface(netif);
            }
        }

        return result;
    }

}
