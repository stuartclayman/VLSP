package usr.net;

import usr.router.Router;
import usr.router.RouterDirectory;
import usr.router.NetIF;
import usr.net.SocketTimeoutException;
import java.net.SocketException;
import java.util.List;
import java.util.ArrayList;


public class NetworkInterface {

    String name;
    Address address;
    int routerPortNo;
    boolean isLocal;
    boolean isClosed;

    /**
     * Construct a NetworkInterface
     */
    NetworkInterface(String name, Address address, int routerPortNo, boolean isLocal, boolean isClosed) {
        this.name = name;
        this.address = address;
        this.routerPortNo = routerPortNo;
        this.isLocal = isLocal;
        this.isClosed = isClosed;
    }



    /**
     * Returns all the interfaces on the Router.. 
     * Returns null if no network interfaces could be found on the Router.
     */
    public static List<NetworkInterface> getNetworkInterfaces() throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        // Get the NetIFs from the fabric
        List<NetIF> netIFs = router.getRouterFabric().listNetIF();

        // Build list of NetworkInterfaces
        List<NetworkInterface> result = new ArrayList<NetworkInterface>();

        for (NetIF netif : netIFs) {
            NetworkInterface nif = new NetworkInterface(netif.getName(), netif.getAddress(),
                                                        netif.getRouterPort().getPortNo(),
                                                        netif.isLocal(), netif.isClosed());

            result.add(nif);
        }

        return result;
    }

    /**
     * Get the interface by address.
     */
    public static NetworkInterface getIFByAddress(Address addr) throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        // Get the NetIFs from the fabric
        List<NetIF> netIFs = router.getRouterFabric().listNetIF();

        NetworkInterface result = null;

        // Build list of NetworkInterfaces
        for (NetIF netif : netIFs) {
            System.out.println("netif = " + netif);

            if (netif.getAddress().equals(addr) || netif.getRemoteRouterAddress().equals(addr)) {
                // found it
                result = new NetworkInterface(netif.getName(), netif.getAddress(),
                                              netif.getRouterPort().getPortNo(),
                                              netif.isLocal(), netif.isClosed());
            }
        }

        return result;
    }

    /**
     * Get the interface by name.
     */
    public static NetworkInterface getIFByName(String name)  throws SocketException {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        // Get the NetIFs from the fabric
        List<NetIF> netIFs = router.getRouterFabric().listNetIF();

        NetworkInterface result = null;

        // Build list of NetworkInterfaces
        for (NetIF netif : netIFs) {
            if (netif.getName().equals(name)) {
                // found it
                result = new NetworkInterface(netif.getName(), netif.getAddress(),
                                              netif.getRouterPort().getPortNo(),
                                              netif.isLocal(), netif.isClosed());
            }
        }

        return result;
    }

    /**
     * Get the interface by index == the router port number.
     */
    public static NetworkInterface getIFByIndex(int index)  throws SocketException  {
        Router router = RouterDirectory.getRouter();

        if (router == null) {
            throw new SocketException("Cannot find Router");
        }

        // Get the NetIFs from the fabric
        List<NetIF> netIFs = router.getRouterFabric().listNetIF();

        NetworkInterface result = null;

        // Build list of NetworkInterfaces
        for (NetIF netif : netIFs) {
            if (netif.getRouterPort().getPortNo() == index) {
                // found it
                result = new NetworkInterface(netif.getName(), netif.getAddress(),
                                              netif.getRouterPort().getPortNo(),
                                              netif.isLocal(), netif.isClosed());
            }
        }

        return result;
    }

    /**
     * Get the name of this NetworkInterface
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Address for this NetworkInterface
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Get the index (router port number) for this NetworkInterface
     */
    public int getIndex() {
        return routerPortNo;
    }

    /**
     * Is closed.
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Is local interface / loopback.
     */
    public boolean isLocal() {
        return isLocal;
    }


}
