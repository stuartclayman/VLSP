package usr.router;

import java.util.*;
import usr.logging.*;
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
    everything on it comes down a given interface -- note the first byte is T*/
    SimpleRoutingTable(byte []bytes, NetIF netif)
        throws Exception
    {
        table_= new HashMap<String,SimpleRoutingTableEntry>();
        SimpleRoutingTableEntry e;
        //System.err.println("Parsing complete Routing table");
        byte []entry= new byte[8];
        if ((bytes.length -1) %8 != 0) {
            Logger.getLogger("log").logln(USR.ERROR, "Received unusual routing table length "+ bytes.length);
        }
        for (int i= 0; i< (bytes.length-1)/8;i++) {
            //System.err.println("Parsing complete Routing table entry at "+i);
            System.arraycopy(bytes,(1+i*8),entry,0,8);
            try {
                e= new SimpleRoutingTableEntry(entry, netif);
                Address a= e.getAddress();
                table_.put(addressAsString(a), e);
            } catch (Exception ex) {
                throw ex;
            }
           // System.err.println("Got entry "+e);
        }
    }
    


    /**
     * The size of the RoutingTable.
     */
    public synchronized int size() {
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
    public synchronized NetIF getInterface(Address addr) 
    {
    
        if (addr == null) {
            return null;
        }
        String a= addressAsString(addr);
        SimpleRoutingTableEntry e= table_.get(a);
        if (e == null) 
            return null;
        return e.getNetIF();
    }
    
    /** A new network interface arrives -- add to
    routing table if necessary return true if change was made */
    public  synchronized boolean addNetIF(NetIF inter, RouterOptions options) {
        //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable: ADD LOCAL NET IF "+inter.getAddress());
        //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable: addNetIF: table before = " + this);

        Address newif= inter.getAddress();
        int weight= inter.getWeight();
        //System.err.println("New entry to router "+newif.toString());
        SimpleRoutingTableEntry e1= new SimpleRoutingTableEntry(newif, 0, null);
        boolean changed1= mergeEntry(e1, null, options); // Add local entry
        //System.err.println("New entry from router "+inter.getRemoteRouterAddress());
        SimpleRoutingTableEntry e2= new SimpleRoutingTableEntry(inter.getRemoteRouterAddress(), 
            0, inter);
        boolean changed2= mergeEntry(e2, inter,options); // Add entry for remote end
        //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable: addNetIF: table after = " + this);
        return changed1 || changed2;
    }
    
    /**
     * Merge a RoutingTable into this one.
     */
    public synchronized boolean mergeTables(SimpleRoutingTable table2, NetIF inter, RouterOptions options) {
       // Logger.getLogger("log").logln(USR.ERROR, "MERGING TABLES");
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
                    String addrStr= addressAsString(a);
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
                        //Logger.getLogger("log").logln(USR.ERROR, "COST CHANGE");
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
            if (mergeEntry(e, inter,options)) {
                changed= true;
            }
        }
        return changed;
    }
    
    /** Get an entry from the table */
    synchronized SimpleRoutingTableEntry getEntry(String a) {
        return table_.get(a);
    } 
    
    /**
     * Merge an entry in this RoutingTable returns true if there has been
     a change
     */
     synchronized boolean  mergeEntry(SimpleRoutingTableEntry newEntry, NetIF inter, RouterOptions options) {

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
      //  Logger.getLogger("log").logln(USR.ERROR, "Weight = "+weight);
        SimpleRoutingTableEntry oldEntry= table_.get(addressAsString(addr));
        // CASE 1 -- NO ENTRY EXISTED
        if (oldEntry == null) {
            
            int newCost= newEntry.getCost() + weight;
            //Logger.getLogger("log").logln(USR.ERROR, "NEW ENTRY "+addr+" cost "+newCost);
            if (newCost > options.getMaxDist()) {
                //Logger.getLogger("log").logln(USR.ERROR, "TOO EXPENSIVE");
                return false;
            } else {
                SimpleRoutingTableEntry e= new SimpleRoutingTableEntry(addr,newCost, inter);
                //Logger.getLogger("log").logln(USR.ERROR, "NEW ENTRY ADDED");
                table_.put(addressAsString(addr),e);    
                return true;
            }
        }
        // CASE 2 -- ENTRY EXISTED BUT WAS MORE EXPENSIVE -- CHEAPER ROUTE FOUND
        int newCost= newEntry.getCost() + weight;
        if (oldEntry.getCost() > newCost) {
            //System.err.println ("CHEAPER ROUTE");
            oldEntry.setCost(newCost);
            oldEntry.setNetIF(inter);
            return true;
        }
        // CASE 3 -- ENTRY EXISTED WAS ON THIS INTERFACE 
        if (inter != null && inter.equals(oldEntry.getNetIF()) && newCost > oldEntry.getCost()) {
            if (newCost > options.getMaxDist()) {
                table_.remove(addr);   // Too far, can no longer route
            } else {
                oldEntry.setCost(newCost);
            }
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
        //Logger.getLogger("log").logln(USR.ERROR, "REMOVE NET IF CALLED");
        ArrayList <String> toRemove= new ArrayList <String>();
        for (SimpleRoutingTableEntry e: getEntries()) {
            //Logger.getLogger("log").logln(USR.ERROR, "TRYING TO REMOVE "+e.getAddress());
            if (netif.equals(e.getNetIF())) {
                String addr= addressAsString(e.getAddress());
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
     * Get an Address as String representation of an Integer
     */
    String addressAsString(Address addr) {
        int id = addr.asInteger();
        return Integer.toString(id);
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
        //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable is:\n"+s);
        return s;
    }
    
     /**
     * To string
     */
    public synchronized byte[] toBytes() {
        // Each entry is encoded by 8 bytes
        //System.err.println("Creating routing table to send");
        Collection<SimpleRoutingTableEntry> rtes= getEntries();
        byte [] bytes= new byte[rtes.size()*8];
        int i= 0;
        for (SimpleRoutingTableEntry e: rtes) {
            //System.err.println("Entry "+e.toString());
            byte []ebytes= e.toBytes();
            System.arraycopy(ebytes,0,bytes,i,8);
            i+= 8;
        } 
        return bytes;
    }
    
}
