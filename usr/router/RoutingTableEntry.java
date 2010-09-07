package usr.router;

class RoutingTableEntry {
    private String routerId_;
    private int cost_;
    NetIF inter_;

    RoutingTableEntry(String id, int cost, NetIF inter) {
        routerId_= id;
        cost_= cost;
        inter_= inter;
    }
    
    RoutingTableEntry(String tableEntry, RouterFabric fabric) {
        String []components= tableEntry.split(" ");
        
        
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
    
    String getEntryAsString() {
        String entry= routerId_ + " " + cost_ + " " + 
            inter_.getRemoteRouterName()+"\n";
        return entry;
    }

}
