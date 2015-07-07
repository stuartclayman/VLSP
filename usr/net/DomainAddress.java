package usr.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.regex.MatchResult;

/**
 * An Domain Address.
 * This is a 2 parts address, with 2 bytes/ 16 bits for a domain,
 * and 2 bytes/ 16 bits for a host.
 */
public class DomainAddress extends Size4 implements Address, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 5297065237202912602L;
    int domainPart;
    int hostPart;

    /**
     * Create a DomainAddress from a String, eg "2.45"
     *
     */
    public DomainAddress(String addrStr)  throws UnknownHostException {
        Scanner scanner = new Scanner(addrStr);

        // match for 2 ints as 2.45
        // match 1 is first part, match 2 is second part
        scanner.findInLine("(\\d+).(\\d+)");
        MatchResult result = scanner.match();

        if (result.groupCount() == 2) {
            String part1 = result.group(1);
            String part2 = result.group(2);

            // now convert string parts into shorts
            // need to tweak the bits to ensure they are 'unsigned'
            scanner.close();
            scanner = new Scanner(part1);
            domainPart = (0) | (scanner.nextShort() & 0xFFFF);
            scanner.close();

            scanner = new Scanner(part2);
            hostPart = (0) | (scanner.nextShort() & 0xFFFF);
            scanner.close();


            // convert int to byte[]
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.putShort((short)domainPart);
            buf.putShort((short)hostPart);



        } else {
            scanner.close();
            throw new UnknownHostException("Not a Domain: " + addrStr);
        }
    }

    /**
     * Create a DomainAddress from an int
     */
    public DomainAddress(int addr) {
        // split int into 2 parts
        int top16 = (addr & 0xFFFF0000) >> 16;
        int bottom16 = addr & 0x0000FFFF;

        domainPart = top16  & 0x0000FFFF;
        hostPart = bottom16  & 0x0000FFFF;


        // convert shorts to byte[]
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putShort((short)domainPart);
        buf.putShort((short)hostPart);
    }

    /**
     * Create a DomainAddress from a byte[]
     */
    public DomainAddress(byte[] addr)  throws UnsupportedOperationException {
        if (addr.length == 4) {
            // copy bytes in
            System.arraycopy(addr, 0, bytes, 0, 4);

            // convert byte[] to int
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            domainPart = buf.getShort();
            hostPart = buf.getShort();

        } else {
            throw new UnsupportedOperationException("DomainAddress: wrong length. Expected 4, got " + addr.length);
        }
    }

    /**
     * Get the domain part of an Address
     */
    public int getDomain() {
        return domainPart;
    }

    /**
     * Get the host part of an Address
     */
    public int getHost() {
        return hostPart;
    }

    /**
     * Get DomainAddress as an Integer.
     */
    @Override
    public int asInteger() {
        int integerView = ((domainPart << 16)) | hostPart;
        return integerView;
    }

    /**
     * Get DomainAddress as an InetAddress
     */
    @Override
    public InetAddress asInetAddress() {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

    /**
     * Address in transmittable form
     */
    @Override
    public String asTransmitForm() {
        return domainPart + "." + hostPart;
    }

    /**
     * Compare this Address to another one
     */
    @Override
    public int compareTo(Object other) {
        int val1 = this.asInteger();
        int val2 = ((Address)other).asInteger();

        if (val1 == val2) {
            return 0;
        } else if (val1 < val2) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Equals
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DomainAddress) {
            DomainAddress addr = (DomainAddress)obj;
            return addr.domainPart == this.domainPart &&
                addr.hostPart == this.hostPart;
        } else {
            return false;
        }
    }

    /**
     * hashcode
     */
    @Override
    public int hashCode() {
        return asTransmitForm().hashCode();
    }

    /**
     * To String
     */
    @Override
    public String toString() {
        return asTransmitForm();
    }

}
