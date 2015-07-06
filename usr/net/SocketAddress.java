package usr.net;

/**
 * A Socket Address has an address and a port.
 */
public class SocketAddress {
    // The address
    Address address;

    // The port
    int port;

    /**
     * Construct a SocketAddress from an address and a port.
     */
    public SocketAddress(Address addr, int port) {
        this.address = addr;
        this.port = port;
    }

    /**
     * Construct a SocketAddress from a port only.
     * Often used in receivers.
     */
    public SocketAddress(int port) {
        this.address = null;
        this.port = port;
    }

    /**
     * Get the Address
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Get the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Equals
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SocketAddress) {
            SocketAddress sockaddr = (SocketAddress)obj;

            if (sockaddr.getAddress().equals(getAddress()) &&
                sockaddr.getPort() == getPort()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * hashCode for SocketAddress
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * To String
     */
    @Override
    public String toString() {
        return address + ":" + port;
    }

}
