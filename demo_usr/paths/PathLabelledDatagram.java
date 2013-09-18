package demo_usr.paths;

import java.nio.ByteBuffer;

import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.Size4;

/**
 * A PathLabelledDatagram holds all the original datagram payload
 * plus some other path info.
 */
public class PathLabelledDatagram {
    // get the payload
    // encapuslation header
    // path id  - 1 byte  - off 0
    // flow id  - 2 bytes -     1
    // flags    - 1 byte  -     3
    // src addr - 4 bytes -     4
    // src port - 2 bytes -     8
    // dst addr - 4 bytes -    10
    // dst port - 2 bytes -    14
    // original data      -    16
    // N bytes
    ByteBuffer buffer;

    /**
     * Construct a PathlabelledDatagram
     */
    PathLabelledDatagram() {
    }

    /**
     * Construct a PathlabelledDatagram from some original data
     * in order to send one
     */
    protected PathLabelledDatagram(byte[] original, int start, int length) {
        // copy original byte [] into new enlarged byte[]
        int dataSize = length;
        byte [] alldata = new byte[dataSize + 16];
        System.arraycopy(original, start, alldata, 16, dataSize);
        // now convert to ByteBuffer
        buffer = ByteBuffer.wrap(alldata);
    }

    protected PathLabelledDatagram(Datagram dg) {
        byte[] payload = dg.getPayload();
        // now convert to ByteBuffer
        buffer = ByteBuffer.wrap(payload);
    }

    /**
     * Convert a Datagram with PathLabelledDatagram payload into a PathLabelledDatagram.
     * All the relevant fields will have been set by the sender.
     */
    public static PathLabelledDatagram fromDatagram(Datagram inDatagram) {
        PathLabelledDatagram dg = new PathLabelledDatagram(inDatagram);
        return dg;
    }

    /**
     * Create a new PathLabelledDatagram from some byte[] data .
     * The creator needs to set all the relevant fields.
     */
    public static PathLabelledDatagram newPathLabelledDatagram(byte[] original) {
        PathLabelledDatagram dg = new PathLabelledDatagram(original, 0, original.length);
        return dg;
    }

    /**
     * Create a new PathLabelledDatagram from some byte[] data .
     * The creator needs to set all the relevant fields.
     */
    public static PathLabelledDatagram newPathLabelledDatagram(byte[] original, int length) {
        PathLabelledDatagram dg = new PathLabelledDatagram(original, 0, length);
        return dg;
    }

    /**
     * Create a new PathLabelledDatagram from some byte[] data .
     * The creator needs to set all the relevant fields.
     */
    public static PathLabelledDatagram newPathLabelledDatagram(byte[] original, int start, int length) {
        PathLabelledDatagram dg = new PathLabelledDatagram(original, start, length);
        return dg;
    }

    /**
     * Get the path label
     */
    public int getPath() {
        byte p = buffer.get(0);
        return 0 | (0xFF & p);
    }
    
    /**
     * Set the path label
     */
    public PathLabelledDatagram setPath(int path)  {
        if (path > 255) {
            throw new Error("Path too big: " + path + ". Max is 255");
        } else {
            byte p = (byte)(path & 0xFF);
            buffer.put(0, p);

            return this;
        }
    }

    /**
     * Get the flow
     */
    public int getFlow() {
        int f = buffer.getShort(1);

        // convert signed to unsigned
        if (f < 0) {
            return f + 65536;
        } else {
            return f;
        }
    }

    /**
     * Set the flow id
     */
    public PathLabelledDatagram setFlow(int flow) {
        if (flow > 65535) {
            throw new Error("Flow too big: " + flow + ". Max is 65535");
        } else {
            buffer.putShort(1, (short)flow);
            return this;
        }

    }
    
    /**
     * Get the flags
     */
    public byte getFlags() {
        byte f = buffer.get(3);
        //return 0 | (0xFF & f);  // if returning int
        return f;
    }
    
    /**
     * Set the flags
     */
    public PathLabelledDatagram setFlags(int flags) {
        byte f = (byte)(flags & 0xFF);
        buffer.put(3, f);

        return this;
    }
    

    /**
     * Get the source address
     */
    public Address getSrcAddress() {
        byte[] srcAddrRaw = new byte[4];

        buffer.position(4);
        buffer.get(srcAddrRaw, 0, 4);

        Address srcAddr = AddressFactory.newAddress(srcAddrRaw);
        return srcAddr;
    }

    /**
     * Set the source address
     */
    public PathLabelledDatagram setSrcAddress(Address addr) {
        // put src addr
        buffer.position(4);

        if (addr == null) {
            buffer.put(Size4.EMPTY, 0, 4);
        } else {
            buffer.put(addr.asByteArray(), 0, 4);
        }

        return this;
    }

    /**
     * Get the source port
     */
    public int getSrcPort() {
        int p = buffer.getShort(8);

        // convert signed to unsigned
        if (p < 0) {
            return p + 65536;
        } else {
            return p;
        }
    }

    /**
     * Set the source port
     */
    public PathLabelledDatagram setSrcPort(int port) {
        buffer.putShort(8, (short)port);

        return this;
    }


    /**
     * Get the destination address
     */
    public Address getDstAddress() {
        byte[] dstAddrRaw = new byte[4];

        buffer.position(10);
        buffer.get(dstAddrRaw, 0, 4);

        Address dstAddr = AddressFactory.newAddress(dstAddrRaw);
        return dstAddr;
    }

    /**
     * Set the destination address
     */
    public PathLabelledDatagram setDstAddress(Address addr) {
        // put dst addr
        buffer.position(10);

        if (addr == null) {
            buffer.put(Size4.EMPTY, 0, 4);
        } else {
            buffer.put(addr.asByteArray(), 0, 4);
        }

        return this;
    }

    /**
     * Get the destination port
     */
    public int getDstPort() {
        int p = buffer.getShort(14);

        // convert signed to unsigned
        if (p < 0) {
            return p + 65536;
        } else {
            return p;
        }
    }

    /**
     * Set the destination port
     */
    public PathLabelledDatagram setDstPort(int port) {
        buffer.putShort(14, (short)port);

        return this;
    }

    /**
     * Get the original data 
     */
    public byte[] getPayload() {
        // the actual data is from 16 to the end
        int dataSize = buffer.capacity() - 16;

        // allocate new byte[] to hold payload
        byte[] data = new byte[dataSize];

        // copy from buffer into data byte[]
        buffer.position(16);
        buffer.get(data);

        return data;
    }

    /**
     * Get the whole PathLabelledDatagram as a byte[]
     */
    public byte[] asByteArray() {
        return buffer.array();
    }
}
