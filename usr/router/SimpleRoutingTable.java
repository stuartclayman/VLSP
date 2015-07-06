package usr.router;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;

/** Class holds a routing table deals with getting updated routing tables
*/
public class SimpleRoutingTable implements RoutingTable {
    // The routing table
    HashMap<Address, SimpleRoutingTableEntry> table_ = null;

    NetIFListener listener_ = null;

    // The size of an address
    int addressSize = 0;

    // The size of a SimpleRoutingTableEntry
    int entrySize = 0;

    /** Construct a new routing table */
    SimpleRoutingTable() {

        table_ = new HashMap<Address, SimpleRoutingTableEntry>();
    }

    /** Construct a new routing table with the assumption that
     * everything on it comes down a given interface -- note the first byte is T
     */
    SimpleRoutingTable(byte [] bytes, NetIF netif)
        throws Exception {
        table_ = new HashMap<Address, SimpleRoutingTableEntry>();
        fromBytes(bytes, netif);
    }

    public synchronized void fromBytes(byte[] bytes, NetIF netif) throws Exception {
        SimpleRoutingTableEntry e;
        //System.err.println("Parsing complete Routing table");

        ByteBuffer wrapper = ByteBuffer.wrap(bytes);

        byte t = wrapper.get();

        if (t != 'T') {
            throw new Exception("SimpleRoutingTable: tried to construct a RoutingTable with invalid data");
        } else {
            wrapper.getShort();
            int entrySize = wrapper.getShort();

            if (entrySize == 0) {
                Logger.getLogger("log").logln(USR.ERROR, "Routing table has entrySize 0");
                throw new Exception("SimpleRoutingTable: tried to construct a RoutingTable with invalid data");
            }

            if ((bytes.length - 5) % entrySize != 0) {
                Logger.getLogger("log").logln(USR.ERROR, "Received unusual routing table length "+ bytes.length);

            } else {

                byte [] entry = new byte[entrySize];

                while (wrapper.hasRemaining()) {
                    // suck out correct no of bytes
                    wrapper.get(entry);

                    // now build SimpleRoutingTableEntry
                    try {
                        e = new SimpleRoutingTableEntry(entry, netif);
                        Address a = e.getAddress();
                        table_.put(a, e);
                    } catch (Exception ex) {
                        throw ex;
                    }
                }
            }
        }
    }

    /** remove address from table.  Return true if address was in table*/
    @Override
    public boolean removeAddress(Address addr) {
        SimpleRoutingTableEntry en = table_.remove(addr);
 
        if (en == null) {
            return false;
        }
 
        return true;
    }
 

    /** Set the NetIFListener */
    @Override
    public void setListener(NetIFListener l) {
        listener_ = l;
    }

    /**
     * The size of the RoutingTable.
     */
    @Override
    public synchronized int size() {
        return table_.size();
    }

    /**
     * Get all the RoutingTable entries.
     */
    @Override
    public synchronized Collection<SimpleRoutingTableEntry> getEntries() {
        return table_.values();
    }

    /** Return the interface on which to send a packet to a given address
        or null if not known */
    @Override
    public synchronized NetIF getInterface(Address addr) {

        if (addr == null) {
            return null;
        }
        SimpleRoutingTableEntry e = table_.get(addr);

        if (e == null) {
            return null;
        } else {
            return e.getNetIF();
        }
    }

    /** A new network interface arrives -- add to
        routing table if necessary return true if change was made */
    @Override
    public synchronized boolean addNetIF(NetIF inter, RouterOptions options) {
        Logger.getLogger("log").logln(USR.STDOUT, "SimpleRoutingTable: ADD LOCAL NET IF "+inter.getAddress());
        //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable: addNetIF: table before = " + this);

        Address a = inter.getAddress();

        // this is the first interface.
        // so we get the size of an Address
        if (table_.size() == 0) {
            addressSize = a.size();
            //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable: set addressSize = " + addressSize);
        }

        // see if the table is changed
        boolean changed1 = false;

        // If necessary add this local address to routing table
        if (table_.get(a) == null) {

            SimpleRoutingTableEntry e1 = new SimpleRoutingTableEntry(a, 0, null);
            table_.put(a, e1);
            changed1 = true;
            entrySize = e1.size();
            //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable: set entrySize = " + entrySize);
        }

        //System.err.println("New entry from router "+inter.getRemoteRouterAddress());

        // Note weight here is 0 because weight of inter will be added by merge
        SimpleRoutingTableEntry e2 = new SimpleRoutingTableEntry(inter.getRemoteRouterAddress(), 0, inter);

        boolean changed2 = mergeEntry(e2, inter, options); // Add entry for remote end

        //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable: addNetIF: table after = " + this);
        return changed1 || changed2;
    }

    /**
     * Merge a RoutingTable into this one.
     */
    @Override
    public synchronized boolean mergeTables(RoutingTable table2, NetIF inter, RouterOptions options) {
        //Logger.getLogger("log").logln(USR.STDOUT, "MERGING TABLES");

        boolean changed = false;
        Collection<SimpleRoutingTableEntry> es = 
            ((SimpleRoutingTable)table2).getEntries();

        // there are no entries
        if (es == null) {
            return false;
        }

        // Check if this table is telling us to remove entries
        ArrayList<Address> toRemove = new ArrayList<Address>();

        // only process entries if inter is not null
        if (inter != null) {
            for (SimpleRoutingTableEntry e : getEntries()) {
                // If this entry is on the same interface we are getting
                // info from but route is longer or no entry then assume
                // we need updating
                Address a = e.getAddress();

                //  Don't update info about our own address
                if (listener_.ourAddress(a)) {
                    //System.err.println("Address is our address");
                    continue;
                }

                // Is interface the same
                if (inter.equals(e.getNetIF())) {

                    SimpleRoutingTableEntry e2 = (SimpleRoutingTableEntry)table2.getEntry(a);

                    // If interface can no longer reach address remove it
                    if (e2 == null) {
                        toRemove.add(a);  // flag removal and do later
                        //System.err.println ("REMOVE");
                        changed = true;
                        continue;
                    }
                    // If cost has become higher add higher cost
                    int receivedCost = e2.getCost()+inter.getWeight();

                    if (receivedCost > e.getCost()) {
                        e.setCost(receivedCost);
                        //Logger.getLogger("log").logln(USR.ERROR, "COST CHANGE");
                        changed = true;
                    }
                }
            }

            // remove entries from this RoutingTable
            for (Address a : toRemove) {
                table_.remove(a);
            }
        }

        // Add new entries as appropriate
        for (SimpleRoutingTableEntry e : ((SimpleRoutingTable) table2).getEntries()) {
            if (mergeEntry(e, inter, options)) {
                changed = true;
            }
        }
        return changed;
    }

    /** Get an entry from the table */
    @Override
    public synchronized SimpleRoutingTableEntry getEntry(Address a) {
        return table_.get(a);
    }

    /**
     * Merge an entry in this RoutingTable returns true if there has been
     a change
    */
    public synchronized boolean  mergeEntry(SimpleRoutingTableEntry newEntry, NetIF inter, RouterOptions options) {

        //Logger.getLogger("log").logln(USR.STDOUT, "MERGING ENTRY");

        if (newEntry == null) {
            //System.err.println ("NULL ENTRY");
            return false;
        }
        /* if (inter == null) {
           System.err.println("Merging entry "+newEntry+" from null");
           } else {
           System.err.println("Merging entry "+newEntry+" from "+inter+" "+inter.getClass());
           }*/
        Address addr = newEntry.getAddress();

        if (listener_.ourAddress(addr)) {
            return false;
        }
        int weight = 0;

        if (inter != null) {
            weight = inter.getWeight();
        }
        //  Logger.getLogger("log").logln(USR.ERROR, "Weight = "+weight);
        // Can't be told more about our address

        SimpleRoutingTableEntry oldEntry = table_.get(addr);

        // CASE 1 -- NO ENTRY EXISTED
        if (oldEntry == null) {

            int newCost = newEntry.getCost() + weight;

            //Logger.getLogger("log").logln(USR.ERROR, "NEW ENTRY "+addr+" cost "+newCost);
            if (newCost > options.getMaxDist()) {
                //Logger.getLogger("log").logln(USR.ERROR, "TOO EXPENSIVE");
                return false;
            } else {
                // Create new entry
                SimpleRoutingTableEntry e = new SimpleRoutingTableEntry(addr, newCost, inter);
                //Logger.getLogger("log").logln(USR.ERROR, "NEW ENTRY ADDED");
                table_.put(addr, e);
                return true;
            }
        }

        // CASE 2 -- ENTRY EXISTED BUT WAS MORE EXPENSIVE -- CHEAPER ROUTE FOUND
        int newCost = newEntry.getCost() + weight;

        if (oldEntry.getCost() > newCost) {
            //System.err.println ("CHEAPER ROUTE");
            oldEntry.setCost(newCost);
            oldEntry.setNetIF(inter);
            return true;
        }

        // CASE 3 -- ENTRY EXISTED WAS ON THIS INTERFACE -- ROUTE GOT MORE EXPENSIVE
        if (inter != null && inter.equals(oldEntry.getNetIF())) {
            if (newCost > oldEntry.getCost()) {
                if (newCost > options.getMaxDist()) {
                    table_.remove(addr);   // Too far, can no longer route
                } else {
                    oldEntry.setCost(newCost);
                }
                //System.err.println ("MORE EXPENSIVE ROUTE BUT SAME INTERFACE");
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    boolean interfaceMatch(NetIF a, NetIF b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    /** Removes a network interface from a router returns true if
        routing table has changed*/
    @Override
    public synchronized boolean removeNetIF(NetIF netif) {
        boolean changed = false;
        //Logger.getLogger("log").logln(USR.ERROR, "REMOVE NET IF CALLED");
        ArrayList<Address> toRemove = new ArrayList<Address>();

        for (SimpleRoutingTableEntry e : getEntries()) {
            //Logger.getLogger("log").logln(USR.ERROR, "TRYING TO REMOVE "+e.getAddress());
            if (netif.equals(e.getNetIF())) {
                Address addr = e.getAddress();
                toRemove.add(addr); // Flag removal and do it later
                changed = true;
            }
        }

        for (Address a : toRemove) {
            table_.remove(a);
        }
        return changed;
    }

    /**
     * Sets a weight on a link on the specified NetIF.
     */
    @Override
    public boolean setNetIFWeight(NetIF inter, int weight) {
        boolean changed = false;

        for (SimpleRoutingTableEntry e : getEntries()) {
            if (inter.equals(e.getNetIF())) {
                //Logger.getLogger("log").logln(USR.STDOUT, "SimpleRoutingTable set weight on " + e);

                // new weight is current weight - interface weight + new interface weight
                int newWeight = e.getCost() - inter.getWeight() + weight;
                e.setCost(newWeight);

                //Logger.getLogger("log").logln(USR.STDOUT, "SimpleRoutingTable set weight on " + e + " to " + newWeight);
                changed = true;
            }
        }

        return changed;
    }


    /**
     * Get an Address as String representation of an Integer
     */
    String addressAsString(Address addr) {
        /*
          int id = addr.asInteger();
          return Integer.toString(id);
        */
        return addr.asTransmitForm();
    }

    /**
     * SHow only data transmitted
     */
    @Override
    public synchronized String showTransmitted() {
        StringBuilder table = new StringBuilder();
        table.append("\n");

        for (SimpleRoutingTableEntry e : getEntries()) {
            table.append(e.showTransmitted());
            table.append("\n");
        }

        String s = table.toString();

        return s;
    }

    /**
     * To string
     */
    @Override
    public synchronized String toString() {
        StringBuilder table = new StringBuilder();
        table.append("\n");

        Collection<SimpleRoutingTableEntry> rtes = getEntries();

        for (SimpleRoutingTableEntry e : rtes) {
            table.append(e.toString());
            table.append("\n");
        }
        String s = table.toString();
        //Logger.getLogger("log").logln(USR.ERROR, "SimpleRoutingTable is:\n"+s);
        return s;
    }

    /**
     * To byte[]
     */
    @Override
    public synchronized byte[] toBytes() {

        // TODO add T to start
        // add size of each entry
        // then add entries

        // Each entry is encoded by addressSize bytes
        //System.err.println("Creating routing table to send");

        Collection<SimpleRoutingTableEntry> rtes = getEntries();
        int count = rtes.size();
        //int entrySize = rtes.iterator().next().size();

        if (entrySize == 0) {
            //Logger.getLogger("log").logln(USR.ERROR, "SRT: entry size is zero!");
            //return null;
            throw new Error("SimpleRoutingTable: entrySize is 0");
        }

        // create a byte[] big enough for
        // T entryCount entrySize all_the_entries_as_byte[]
        // 5 + (entrySize * no_of_entires)
        byte [] bytes = new byte[5 + entrySize*count];

        ByteBuffer wrapper = ByteBuffer.wrap(bytes);

        wrapper.put((byte)'T');
        wrapper.putShort((short)count);
        wrapper.putShort((short)entrySize);

        for (SimpleRoutingTableEntry e : rtes) {
            byte [] ebytes = e.toBytes();
            wrapper.put(ebytes);
        }

        return bytes;
    }

}
