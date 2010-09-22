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
        throws Exception
    {
        table_= new HashMap<String,SimpleRoutingTableEntry>();
        String []entries= table.split("\n");
        SimpleRoutingTableEntry e;
        for (String s: entries) {
            try {
                e= new SimpleRoutingTableEntry(s, netif);
                Address a= e.getAddress();
                table_.put(a.toString(), e);
            } catch (Exception ex) {
                throw ex;
            }
            
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
        // Check if this table is telling us to remove entries
        if (inter != null) {
            for (SimpleRoutingTableEntry e: getEntries()) {
                // If this entry is on the same interface we are getting
                // info from but route is longer or no entry then assume
                // we need updating
                if (inter.equals(e.getNetIF())) {
                    Address a= e.getAddress();
                    String addrStr= a.toString();
                    SimpleRoutingTableEntry e2= table2.getEntry(addrStr);
                    // If interface can no longer reach address remove it
                    if (e2 == null) {
                        table_.remove(addrStr);
                        changed= true;
                        continue;
                    }
                    // If cost has become higher add higher cost
                    int receivedCost= e2.getCost()+inter.getWeight();
                    if (receivedCost > e.getCost()) {
                        SimpleRoutingTableEntry e3= new SimpleRoutingTableEntry(a,receivedCost,inter);
                        table_.put(addrStr,e3);
                        changed= true;
                    }
                }
            }
        }
        // Add new entries as appropriate
        for (SimpleRoutingTableEntry e: table2.getEntries()) {
            if (mergeEntry(e, inter)) {
                changed= true;
            }
        }
        return changed;
    }
    
    /** Get an entry from the table */
    SimpleRoutingTableEntry getEntry(String a) {
        return table_.get(a);
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
    
    /** Removes a network interface from a router returns true if 
    routing table has changed*/
    public synchronized  boolean removeNetIF(NetIF netif) {
        boolean changed= false;
        for (SimpleRoutingTableEntry e: getEntries()) {
            if (netif.equals(e.getNetIF())) {
                String addr= e.getAddress().toString();
                table_.remove(addr); 
                changed= true;
            }
        }
        return changed;
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
