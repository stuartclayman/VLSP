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
        System.err.println("SimpleRoutingTable: ADD LOCAL NET IF "+inter.getAddress());
        System.err.println("SimpleRoutingTable: addNetIF: table before = " + this);

        Address newif= inter.getAddress();
        int weight= inter.getWeight();
        SimpleRoutingTableEntry e= new SimpleRoutingTableEntry(newif, 0, null);
        boolean changed= mergeEntry(e, null); // Add local entry

        System.err.println("SimpleRoutingTable: addNetIF: table after = " + this);
        return changed;
    }
    
    /**
     * Merge a RoutingTable into this one.
     */
    public synchronized boolean mergeTables(SimpleRoutingTable table2, NetIF inter) {
       // System.err.println("MERGING TABLES");
        boolean changed= false;
        Collection <SimpleRoutingTableEntry> es= table2.getEntries();
        if (es == null) {
            return false;
        }

        ArrayList <String> toRemove= new ArrayList <String>();
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
                        toRemove.add(addrStr);  // flag removal and do later
                        //System.err.println ("REMOVE");
                        changed= true;
                        continue;
                    }
                    // If cost has become higher add higher cost
                    int receivedCost= e2.getCost()+inter.getWeight();
                    if (receivedCost > e.getCost()) {
                        e.setCost(receivedCost);
                        //System.err.println("COST CHANGE");
                        changed= true;
                    }
                }
            }
            // 
            for (String a: toRemove) {
                table_.remove(a);
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
        // CASE 1 -- NO ENTRY EXISTED
        if (oldEntry == null) {
            SimpleRoutingTableEntry e= new SimpleRoutingTableEntry(addr,newEntry.getCost() +
               weight, inter);
           // System.err.println("NEW ENTRY");
            table_.put(addr.toString(),e);    
            return true;
        }
        // CASE 2 -- ENTRY EXISTED BUT WAS MORE EXPENSIVE
        int newCost= newEntry.getCost() + weight;
        if (oldEntry.getCost() > newCost) {
            //System.err.println ("CHEAPER ROUTE");
            oldEntry.setCost(newCost);
            oldEntry.setNetIF(inter);
            return true;
        }
        // CASE 3 -- ENTRY EXISTED WAS ON THIS INTERFACE 
        if (inter != null && inter.equals(oldEntry.getNetIF()) && newCost > oldEntry.getCost()) {
            oldEntry.setCost(newCost);
            //System.err.println ("MORE EXPENSIVE ROUTE BUT SAME INTERFACE");
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
        //System.err.println("REMOVE NET IF CALLED");
        ArrayList <String> toRemove= new ArrayList <String>();
        for (SimpleRoutingTableEntry e: getEntries()) {
            //System.err.println("TRYING TO REMOVE "+e.getAddress());
            if (netif.equals(e.getNetIF())) {
                String addr= e.getAddress().toString();
                toRemove.add(addr);// Flag removal and do it later
                changed= true;
            }
        }
        
        for (String a: toRemove) {
            table_.remove(a);
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
