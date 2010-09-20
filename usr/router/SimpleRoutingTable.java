package usr.router;

import java.util.*;
import usr.net.*;

/** Class holds a routing table deals with getting updated routing
tables
*/
public class SimpleRoutingTable implements RoutingTable {
    // The routing table
    HashMap <String, SimpleRoutingTableEntry> table_= null;
    
    /** Construct a new routing table */
    SimpleRoutingTable() {
        table_= new HashMap<String,SimpleRoutingTableEntry>();
        
    }
    
    /** Construct a new routing table from a string assuming
     the table is centred here */ 
    SimpleRoutingTable(String table, RouterFabric fabric)
    {
        table_= new HashMap<String,SimpleRoutingTableEntry>();
        String []entries= table.split("\n");
        SimpleRoutingTableEntry e;
        
        for (String s: entries) {
            e= new SimpleRoutingTableEntry(s, fabric);
            Address a= e.getAddress();
            table_.put(a.toString(), e);
        }
    }
    
    /** Construct a new routing table with the assumption that
    everything on it comes down a given interface */
    SimpleRoutingTable(String table, NetIF netif)
    {
        table_= new HashMap<String,SimpleRoutingTableEntry>();
        String []entries= table.split("\n");
        SimpleRoutingTableEntry e;
        for (String s: entries) {
            e= new SimpleRoutingTableEntry(s, netif);
            Address a= e.getAddress();
            table_.put(a.toString(), e);
        }
    }

    /**
     * The size of the RoutingTable.
     */
    public int size() {
        return table_.size();
    }

    /**
     * Get all the RoutingTable entries.
     */
    public Collection<SimpleRoutingTableEntry> getEntries() 
    {
        return table_.values();    
    }
    
    /** Address belonging to this router is added */
    public void addMyAddress(Address a)
    {
        //System.err.println("ADD MY ADDRESS");
        SimpleRoutingTableEntry e= new SimpleRoutingTableEntry(a, 0, null);
        mergeEntry(e,null);
    }
    
    /** A new network interface arrives -- add to
    routing table if necessary */
    public void addNetIF(NetIF inter) {
       // System.err.println("ADD LOCAL NET IF");
        Address newif= inter.getAddress();
        int weight= inter.getWeight();
        SimpleRoutingTableEntry e= new SimpleRoutingTableEntry(newif, 0, null);
        mergeEntry(e, null); // Add local entry
        inter.sendRoutingTable(toString());
    }
    
    /**
     * Merge a RoutingTable into this one.
     */
    public void mergeTables(SimpleRoutingTable table2, NetIF inter) {
       // System.err.println("MERGING TABLES");
        Collection <SimpleRoutingTableEntry> es= table2.getEntries();
        if (es == null)
            return;
        for (SimpleRoutingTableEntry e: table2.getEntries()) {
            mergeEntry(e, inter);
        }
    }
    
    /**
     * Merge an entry in this RoutingTable.
     */
    void mergeEntry(SimpleRoutingTableEntry newEntry, NetIF inter) {
        if (newEntry == null) {
            //System.err.println ("NULL ENTRY");
            return;
        }
        Address addr= newEntry.getAddress();
        
        int weight= 0;
        if (inter == null) 
            weight= 0;
        else
            weight= inter.getWeight();
      //  System.err.println("Weight = "+weight);
        SimpleRoutingTableEntry oldEntry= table_.get(addr.toString());
        // Update entry if no entry exists, if new entry is cheaper
        // or if newEntry is on same interface (indicating an
        // increase in cost
        //System.err.println("Merging new entry into routing table "+newEntry);
      //  if (oldEntry == null) {
      //    System.err.println("NO PREVIOUS ENTRY for addrss "+addr);
       // } else {
       //   System.err.println("Previous entry "+oldEntry);        
       // }
        if (oldEntry == null || interfaceMatch(oldEntry.getNetIF(),inter)
            || oldEntry.getCost() > 
            newEntry.getCost() + weight) {
            SimpleRoutingTableEntry e= new SimpleRoutingTableEntry(addr,newEntry.getCost() +
               weight, inter);
           // System.err.println("MERGING ENTRY TO "+addr+" cost "+newEntry.getCost() + " "+
           //    weight);
            table_.put(addr.toString(),e);    
        }
    }
    
    boolean interfaceMatch(NetIF a, NetIF b) {
       if (a == null && b == null)
           return true;
       if (a == null || b == null)
           return false;
       return a.equals(b);
    }
    
    public void removeNetIF(NetIF netif) {
        System.err.println("to write removeNetIF");
    }
    
    /**
     * To string
     */
    public String toString() {
        StringBuilder table= new StringBuilder();
        for (SimpleRoutingTableEntry e: getEntries()) {
            table.append(e.toString());
            table.append("\n");
        }
        String s= table.toString();
        //System.err.println("STRING IS "+s);
        return s;
    }
}
