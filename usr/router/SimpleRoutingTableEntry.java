package usr.router;

/**
 * An entry in a routing table.
 */
public class SimpleRoutingTableEntry implements RoutingTableEntry {
    private String routerId_;
    private int cost_;
    NetIF inter_;

    SimpleRoutingTableEntry(String id, int cost, NetIF inter) {
        routerId_= id;
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
        routerId_= args[0];
        try {
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
        routerId_= args[0];
        try {
            cost_= Integer.parseInt(args[1]);
            inter_= inter;
            
        } catch (Exception e) {
            System.err.println("Attempt to construct routing table entry "+
             "from incorrect string" + tableEntry);
            System.exit(-1);
        }
    }
    
    String getRouterId() {
        return routerId_;
    }
    
    NetIF getNetIF() {
        return inter_;
    }
    
    int getCost() {
        return cost_;
    }
    void setCost(int cost) 
    {
        cost_= cost;
    }
    
    public String getEntryAsString() {
        return this.toString();
    }
     
    public String toString() {
        String entry= routerId_ + " " + cost_ + " " + 
            inter_.getRemoteRouterName();
        return entry;
    }

}
