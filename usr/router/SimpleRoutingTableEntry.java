package usr.router;

import usr.net.*;

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
    
    SimpleRoutingTableEntry(String tableEntry, RouterFabric fabric) {
        String []args= tableEntry.split(" ");
        if (args.length != 3) {
            System.err.println("Attempt to construct routing table entry "+
             "from incorrect string" + tableEntry);
            System.exit(-1);
        }
        try {
            address_= new SimpleAddress(args[0]); //TODO FIX THIS
            cost_= Integer.parseInt(args[1]);
            inter_= fabric.findNetIF(args[2]);
            
        } catch (Exception e) {
            System.err.println("Attempt to construct routing table entry "+
             "from incorrect string" + tableEntry);
            System.exit(-1);
        }
    }
    
     SimpleRoutingTableEntry(String tableEntry, NetIF inter) {
        String []args= tableEntry.split(" ");
        if (args.length != 3) {
            System.err.println("Attempt to construct routing table entry "+
             "from incorrect string" + tableEntry);
            System.exit(-1);
        }
        
        try {
            address_= new SimpleAddress(args[0]); //TODO FIX THIS
            cost_= Integer.parseInt(args[1]);
            inter_= inter;
            
        } catch (Exception e) {
            System.err.println("Attempt to construct routing table entry "+
             "from incorrect string" + tableEntry);
            System.exit(-1);
        }
    }
    
    public Address getAddress() {
        return address_;
    }
    
    public NetIF getNetIF() {
        return inter_;
    }
    
    int getCost() {
        return cost_;
    }
    void setCost(int cost) 
    {
        cost_= cost;
    }
    
    
     
    public String toString() {
        String entry= address_ + " " + cost_ + " " + 
            inter_.getRemoteRouterName();
        return entry;
    }

}
