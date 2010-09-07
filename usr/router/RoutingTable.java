package usr.router;

import java.util.*;

/** Class holds a routing table 
*/
class RoutingTable {
    
    HashMap <String, RoutingTableEntry> table_= null;
    
    RoutingTable() {
        table_= new HashMap<String,RoutingTableEntry>();
        
    }
    
    /** A new network interface arrives -- add to
    routing table if necessary */
    void addNetIF(NetIF inter) {
        String router= inter.getRemoteRouterName();
        int weight= inter.getWeight();
        RoutingTableEntry e= table_.get(router);
        if (e == null || e.getCost() > weight) {
            e= new RoutingTableEntry(router,weight,inter);
            table_.put(router, e);
        }
    }
    
    void mergeTables(RoutingTable table2, NetIF inter) {
        for (RoutingTableEntry e: table2.getEntries()) {
            mergeEntry(e, inter);
        }
    }
    
    private Collection <RoutingTableEntry> getEntries() 
    {
        return table_.values();    
    }
    
    void mergeEntry(RoutingTableEntry newEntry, NetIF inter) {
        String router= inter.getRemoteRouterName();
        int weight= inter.getWeight();
        RoutingTableEntry oldEntry= table_.get(router);
        if (oldEntry == null || oldEntry.getCost() > 
            newEntry.getCost() + weight) {
            oldEntry= new RoutingTableEntry(router,newEntry.getCost() +
               weight, inter);
            table_.put(router,oldEntry);    
        }
    }
    
    public String listRoutingTable() {
        String table= new String();
        for (RoutingTableEntry e: getEntries()) {
            table+= e.getEntryAsString();
        }
        return table;
    }
}
