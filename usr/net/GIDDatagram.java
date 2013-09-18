package usr.net;

import java.nio.ByteBuffer;

/**
 * A simple implementation of a GID Datagram.
 */
public class GIDDatagram extends Size4Datagram implements Datagram, DatagramPatch {
    /**
     * Construct a GIDDatagram given a payload.
     */
    GIDDatagram(ByteBuffer payload) {
        super(payload);
    }

    /**
     * Construct a GIDDatagram given a payload.
     */
    GIDDatagram(byte[] payload) {
        super(payload);
    }

    /**
     * Construct a GIDDatagram given a payload and a destination address
     */
    GIDDatagram(ByteBuffer payload, Address address) {
        super(payload, address);
    }

    /**
     * Construct a GIDDatagram given a payload and a destination address
     */
    GIDDatagram(byte[] payload, Address address) {
        super(payload, address);
    }

    /**
     * Construct a GIDDatagram given a payload, a destination address,
     * and a destination port.
     */
    GIDDatagram(ByteBuffer payload, Address address, int port) {
        super(payload, address, port);
    }

    /**
     * Construct a GIDDatagram given a payload, a destination address,
     * and a destination port.
     */
    GIDDatagram(byte[] payload, Address address, int port) {
        super(payload, address, port);
    }

    GIDDatagram() {
        super();
    }

    /**
     * Get src address.
     * This forcible returns a GIDAddress, irrespective
     * of the AddressFactory settings.
     */
    @Override
	public Address getSrcAddress() {
        // get 4 bytes for address
        byte[] address = new byte[4];
        fullDatagram.position(10);
        fullDatagram.get(address, 0, 4);

        if (emptyAddress(address)) {
            return null;
        }
        return new GIDAddress(address);
    }

    /**
     * Get dst address.
     * This forcible returns a GIDAddress, irrespective
     * of the AddressFactory settings.
     */
    @Override
	public Address getDstAddress() {
        // get 4 bytes for address
        //if (dstAddr == null)
        //   return null;
        byte[] address = new byte[4];
        fullDatagram.position(14);
        fullDatagram.get(address, 0, 4);

        if (emptyAddress(address)) {
            return null;
        }
        return new GIDAddress(address);
    }

}