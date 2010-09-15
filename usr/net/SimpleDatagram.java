package usr.net;

import java.io.*;
import java.nio.*;

/**
 * A simple implementation of a Datagram.
 */
public class SimpleDatagram implements Datagram, DatagramPatch {

    byte protocol_= 0;

    // The full datagram contents
    byte [] payload_= null;

    // Src address
    Address srcAddr_ = null;

    // Src port
    int srcPort_ = 0;

    // Dst address
    Address dstAddr_ = null;

    // Dst port
    int dstPort_ = 0;



    /**
     * Construct a SimpleDatagram given a payload.
     */
    public SimpleDatagram(ByteBuffer payload) {
        payload_= payload.array();
    }

    /**
     * Construct a SimpleDatagram given a payload and a destination address
     */
    public SimpleDatagram(ByteBuffer payload, Address address) {
        payload_= payload.array();
        dstAddr_ = address;
    }

    /**
     * Construct a SimpleDatagram given a payload, a destination address,
     * and a destination port.
     */
    public SimpleDatagram(ByteBuffer payload, Address address, int port) {
        payload_ = payload.array();
        dstAddr_ = address;
        dstPort_ = port;
    }

    SimpleDatagram() {
    
    }

    /**
     * Get the header len
     */
    public byte getHeaderLength() {
        int len= getTotalLength() - getPayloadLength();
        return (byte)len;
    }

    public int getPayloadLength() {
        if (payload_ == null)
            return 0;
        return payload_.length;
    }

    /**
     * Get the total len
     */
    public short getTotalLength() {
        return (short)toByteArray().length;
    }

    /**
     * Get the checksum size
     */
    public byte getChecksumLength() {
        return 0;
    }


    /**
     * Get the flags
     */
    public byte getFlags() {
        return 0;
    }

    /**
     * Get the TTL
     */
    public int getTTL() {
        return 255;
    }

    /**
     * Get the protocol
     */
    public byte getProtocol() {
        return (byte)protocol_;
    }

    /**
     * Set the protocol
     */
    public Datagram setProtocol(int p) {
        protocol_= (byte)p;
        return this;
    }


    /**
     * Get src address.
     */
    public Address getSrcAddress() {
        return srcAddr_;
    }

    /**
     * Set the src address
     */
    public Datagram setSrcAddress(Address addr) {
        srcAddr_ = addr;
        return this;
    }

    /**
     * Get dst address.
     */
    public Address getDstAddress() {
        return dstAddr_;
    }

    /**
     * Set the dst address
     */
    public Datagram setDstAddress(Address addr) {
        dstAddr_ = addr;
        return this;
    }


    /**
     * Get src port.
     */
    public int getSrcPort() {
        return srcPort_;
    }

    /**
     * Set the src port
     */
    public Datagram setSrcPort(int p) {
        srcPort_ = p;
        return this;
    }

    /**
     * Get dst port.
     */
    public int getDstPort() {
        return dstPort_;
    }

    /**
     * Set the dst port
     */
    public Datagram setDstPort(int p) {
        dstPort_ = p;
        return this;
    }

    /**
     * Get header
     */
    public byte[] getHeader() {
        return null;
    }


    /**
     * Get payload
     */
    public byte[] getPayload() {
        return payload_;
        
    }

    /**
     * Get the checksum
     */
    public byte[] getChecksum() {
        return null;
    }
    
    public byte[] toByteArray() {
        byte[] buf = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
            ObjectOutputStream out = new ObjectOutputStream(bos); 
            out.writeObject(this); 
            out.close(); // Get the bytes of the serialized object 
            buf = bos.toByteArray();
        } catch (java.io.IOException e) {
        
        }
        return buf;
    }

    /**
     * To ByteBuffer.
     */
    public ByteBuffer toByteBuffer() {
        return  ByteBuffer.wrap(toByteArray());

    }

    /**
     * From ByteBuffer.
     */
    public boolean fromByteBuffer(ByteBuffer b) {
        byte[] by= b.array();
        Object o= null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(by); 
            ObjectInputStream in = new ObjectInputStream(bis); 
            o= in.readObject(); 
            in.close(); // Get the bytes of the serialized object 
           
        } catch (Exception e) {
        
        }
        SimpleDatagram g= (SimpleDatagram) o;
        this.payload_= g.getPayload();
        this.protocol_= g.getProtocol();
        this.srcAddr_= g.getSrcAddress();
        this.srcPort_= g.getSrcPort();
        this.dstAddr_= g.getDstAddress();
        this.dstPort_= g.getDstPort();
        return true;
    }

}
