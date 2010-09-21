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
    public synchronized Collection<SimpleRoutingTableEntry> getEntries() 
    {
        return table_.values();    
    }
    
  
    
    /** Return the interface on which to send a packet to a given address
    or null if not known */
    public NetIF getInterface(Address addr) 
    {
        String a= addr.toString();
        SimpleRoutingTableEntry e= table_.get(a);
        if (e == null) 
            return null;
        return e.getNetIF();
    }
    
    /** A new network interface arrives -- add to
    routing table if necessary return true if change was made */
    public  synchronized boolean addNetIF(NetIF inter) {
        //System.err.println("ADD LOCAL NET IF "+inter.getAddress());
        Address newif= inter.getAddress();
        int weight= inter.getWeight();
        SimpleRoutingTableEntry e= new SimpleRoutingTableEntry(newif, 0, null);
        boolean changed= mergeEntry(e, null); // Add local entry
        inter.sendRoutingTable(toString(),true);
        return changed;
    }
    
    /**
     * Merge a RoutingTable into this one.
     */
    public synchronized boolean mergeTables(SimpleRoutingTable table2, NetIF inter) {
       // System.err.println("MERGING TABLES");
        boolean changed= false;
        Collection <SimpleRoutingTableEntry> es= table2.getEntries();
        if (es == null)
            return false;
        for (SimpleRoutingTableEntry e: table2.getEntries()) {
            if (mergeEntry(e, inter)) {
                changed= true;
            }
        }
        return changed;
    }
    
    /**
     * Merge an entry in this RoutingTable returns true if there has been
     a change
     */
     synchronized boolean  mergeEntry(SimpleRoutingTableEntry newEntry, NetIF inter) {

        if (newEntry == null) {
            //System.err.println ("NULL ENTRY");
            return false;
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
            return true;
        }
        return false;
    }
    
    boolean interfaceMatch(NetIF a, NetIF b) {
       if (a == null && b == null)
           return true;
       if (a == null || b == null)
           return false;
       return a.equals(b);
    }
    
    /** Removes a network interface from a router */
    public synchronized  boolean removeNetIF(NetIF netif) {
        System.err.println("to write removeNetIF");
        return false;
    }
    
    /**
     * To string
     */
    public synchronized String toString() {
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
