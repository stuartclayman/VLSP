package usr.router;

import usr.net.*;
import usr.logging.*;
import java.nio.ByteBuffer;

/**
 * An entry in a routing table.
 */
public class SimpleRoutingTableEntry implements RoutingTableEntry {
    private Address address_;
    private int cost_;
    NetIF inter_;

    SimpleRoutingTableEntry(Address addr, int cost, NetIF inter) {
        address_= addr;
        cost_= cost;
        inter_= inter;
    }
    
    SimpleRoutingTableEntry(byte []tableEntry, NetIF inter)  
      throws Exception
    {
        if (tableEntry.length < 8) {
            throw new Exception
             ("Byte array received to construct routing table too short");
        }
        ByteBuffer bytes= ByteBuffer.wrap(tableEntry);
        address_ = AddressFactory.newAddress(bytes.getInt(0));
        cost_ = bytes.getInt(4);
        inter_= inter;
        //System.err.println("NEW ENTRY CREATED "+toString());
    }


    
    public Address getAddress() {
        return address_;
    }
    
    public NetIF getNetIF() {
        return inter_;
    }
    
    /** Setter function for network interface */
    public void setNetIF(NetIF i) {
      inter_= i;
    }
    
    public int getCost() {
        return cost_;
    }

    void setCost(int cost) 
    {
        cost_= cost;
    }
    
    /**
     * Get an Address as String representation of an Integer
     */
    String addressAsString(Address addr) {
        int id = addr.asInteger();
        return Integer.toString(id);
    }
        
    public byte [] toBytes() {
        byte []bytes= new byte[8];
        ByteBuffer b= ByteBuffer.wrap(bytes);
        b.putInt(0,address_.asInteger());
        b.putInt(4,cost_);
        return bytes;
    }        
        
    /** Entry represented as string */ 
    public String toString() {
        String entry;
        if (inter_ == null) {
            entry= addressAsString(address_) + " " + cost_+ " nullIF";
        } else {
            entry= addressAsString(address_) + " " + cost_ + " IF: " + 
            inter_;
        }
        //Logger.getLogger("log").logln(USR.ERROR, "ENTRY: "+entry);
        return entry;
    }

}
