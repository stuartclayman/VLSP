package usr.router;

import java.nio.ByteBuffer;

import usr.net.Address;
import usr.net.AddressFactory;

/**
 * An entry in a routing table.
 */
public class SimpleRoutingTableEntry implements RoutingTableEntry {
    private Address address_;
    private int cost_;
    // NetIF of null is a local interface
    private NetIF inter_;

    SimpleRoutingTableEntry(Address addr, int cost, NetIF inter) {
        address_ = addr;
        cost_ = cost;
        inter_ = inter;
    }

    SimpleRoutingTableEntry(byte [] tableEntry, NetIF inter)
    throws Exception {
        if (tableEntry.length < 8) {
            throw new Exception
                      ("Byte array received to construct routing table too short");
        }
        ByteBuffer wrapper = ByteBuffer.wrap(tableEntry);

        // get correct no of bytes for an address
        int totalLength = tableEntry.length;
        int costStart = totalLength - 4;  // subtract length of cost

        //System.err.println("SimpleRoutingTableEntry: tableEntry size = " + totalLength);

        // suck out the address
        byte[] addr = new byte[costStart];
        wrapper.get(addr);
        address_ = AddressFactory.newAddress(addr);

        // now get the cost
        wrapper.position(costStart);
        cost_ = wrapper.getInt();
        inter_ = inter;
        //System.err.println("NEW ENTRY CREATED "+toString());
    }

    @Override
	public Address getAddress() {
        return address_;
    }

    @Override
	public NetIF getNetIF() {
        return inter_;
    }

    /** Setter function for network interface */
    public void setNetIF(NetIF i) {
        inter_ = i;
    }

    @Override
	public int getCost() {
        return cost_;
    }

    void setCost(int cost) {
        cost_ = cost;
    }

    /**
     * Get an Address as String representation of an Integer
     */
    String addressAsString(Address addr) {
        /*
           int id = addr.asInteger();
           return Integer.toString(id);
         */
        //return addr.asTransmitForm();
        return addr.toString();
    }

    /**
     * The size in bytes of a RoutingTableEntry.
     */
    @Override
	public int size() {
        // the size of the address, plus 4 for the cost
        return address_.asByteArray().length + 4;
    }

    /**
     * Transform as SimpleRoutingTableEntry into a byte[]
     */
    public byte [] toBytes() {
        byte [] bytes = new byte[size()];
        ByteBuffer b = ByteBuffer.wrap(bytes);
        // copy in the address
        b.put(address_.asByteArray());
        b.putInt(cost_);
        return bytes;
    }

    /**
     * SHow only data transmitted
     */
    @Override
	public String showTransmitted() {
        String entry = "[ ";

        entry += addressAsString(address_) + " W(" + cost_+ ") ";

        entry += " ]";

        return entry;

    }

    /** Entry represented as string */
    @Override
	public String toString() {
        String entry = "[ ";

        if (inter_ == null) {
            entry += addressAsString(address_) + " W(" + cost_+ ") IF: localhost";
        } else {
            entry += addressAsString(address_) + " W(" + cost_ + ") IF: " + ("if" + portNo(inter_))  + " => " + addressAsString(
                    inter_.getRemoteRouterAddress());
        }

        entry += " ]";
        //Logger.getLogger("log").logln(USR.ERROR, "ENTRY: "+entry);
        return entry;
    }

    private String portNo(NetIF inter) {
        if (inter.getRouterPort() == null) {
            return "_temp";
        } else {
            return Integer.toString(inter.getRouterPort().getPortNo());
        }
    }

}