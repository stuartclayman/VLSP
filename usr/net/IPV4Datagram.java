package usr.net;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * A simple implementation of a IPV4 Datagram.
 */
public class IPV4Datagram extends Size4Datagram implements Datagram, DatagramPatch {
    /**
     * Construct a IPV4Datagram given a payload.
     */
    IPV4Datagram(ByteBuffer payload) {
        super(payload);
    }

    /**
     * Construct a IPV4Datagram given a payload.
     */
    IPV4Datagram(byte[] payload) {
        super(payload);
    }

    /**
     * Construct a IPV4Datagram given a payload and a destination address
     */
    IPV4Datagram(ByteBuffer payload, Address address) {
        super(payload, address);
    }

    /**
     * Construct a IPV4Datagram given a payload and a destination address
     */
    IPV4Datagram(byte[] payload, Address address) {
        super(payload, address);
    }

    /**
     * Construct a IPV4Datagram given a payload, a destination address,
     * and a destination port.
     */
    IPV4Datagram(ByteBuffer payload, Address address, int port) {
        super(payload, address, port);
    }

    /**
     * Construct a IPV4Datagram given a payload, a destination address,
     * and a destination port.
     */
    IPV4Datagram(byte[] payload, Address address, int port) {
        super(payload, address, port);
    }

    IPV4Datagram() {
        super();
    }

    /**
     * Get src address.
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
        try {
            return new IPV4Address(address);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

    /**
     * Get dst address.
     */
    @Override
	public Address getDstAddress() {
        // get 4 bytes for address
        byte[] address = new byte[4];
        fullDatagram.position(14);
        fullDatagram.get(address, 0, 4);

        if (emptyAddress(address)) {
            return null;
        }
        try {
            return new IPV4Address(address);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

}