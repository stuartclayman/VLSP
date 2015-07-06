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
public class VectorRoutingTable implements RoutingTable {
    // The routing table
    HashMap<Address, VectorRoutingTableEntry> table_ = null;

    NetIFListener listener_ = null;

    // The size of an address
    int addressSize = 0;

    // The size of a VectorRoutingTableEntry
    int entrySize = 0;

    // the total no of topologies
    int totalTopology = 0;

    // the current chosen topology
    int currentTopology = 0;


    /** Construct a new VectorRoutingTable
     * with a number of topologies
     */
    VectorRoutingTable(int t) {
        table_ = new HashMap<Address, VectorRoutingTableEntry>();
        totalTopology = t;
    }

    /** Construct a new routing table with the assumption that
     * everything on it comes down a given interface -- note the first byte is T
     */
    VectorRoutingTable(byte [] bytes, NetIF netif) throws Exception {
        table_ = new HashMap<Address, VectorRoutingTableEntry>();
        fromBytes(bytes, netif);
    }

    /**
     * Get the current topology we are using
     */
    public int getTopology() {
        return currentTopology;
    }

    /**
     * Set the current topology we are using
     */
    public void setTopology(int t) {
        if (t >= totalTopology) {
            throw new Error("Cannot set topology to " + t + ". Max is " + (totalTopology-1));
        } else {
            currentTopology = t;
        }
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

        Collection<VectorRoutingTableEntry> rtes = 
            getEntries();
        int count = rtes.size();
        //int entrySize = rtes.iterator().next().size();

        if (entrySize == 0) {
            //Logger.getLogger("log").logln(USR.ERROR, "SRT: entry size is zero!");
            //return null;
            throw new Error("VectorRoutingTable: entrySize is 0");
        }

        // create a byte[] big enough for
        // T entryCount entrySize all_the_entries_as_byte[]
        // 6 + (entrySize * no_of_entires)
        byte [] bytes = new byte[6 + entrySize*count];

        ByteBuffer wrapper = ByteBuffer.wrap(bytes);

        wrapper.put((byte)'T');
        wrapper.putShort((short)count);
        wrapper.putShort((short)entrySize);
        wrapper.put((byte)(addressSize & 0xFF));

        for (VectorRoutingTableEntry e : rtes) {
            byte [] ebytes = e.toBytes();
            wrapper.put(ebytes);
        }

        return bytes;
    }

    /**
     * From bytes
     */
    public synchronized void fromBytes(byte[] bytes, NetIF netif) throws Exception {
        VectorRoutingTableEntry e;
        //System.err.println("Parsing complete Routing table");

        ByteBuffer wrapper = ByteBuffer.wrap(bytes);

        byte t = wrapper.get();

        if (t != 'T') {
            throw new Exception("VectorRoutingTable: tried to construct a RoutingTable with invalid data");
        } else {
            wrapper.getShort();
            int entrySize = wrapper.getShort();
            addressSize = wrapper.get();

            if (entrySize == 0) {
                Logger.getLogger("log").logln(USR.ERROR, "Routing table has entrySize 0");
                throw new Exception("VectorRoutingTable: tried to construct a RoutingTable with invalid data");
            }

            if ((bytes.length - 6) % entrySize != 0) {
                Logger.getLogger("log").logln(USR.ERROR, "Received unusual routing table length "+ bytes.length);

            } else {

                byte [] entry = new byte[entrySize];

                while (wrapper.hasRemaining()) {
                    // suck out correct no of bytes
                    wrapper.get(entry);

                    // now build VectorRoutingTableEntry
                    try {
                        e = new VectorRoutingTableEntry(addressSize, entry, netif);
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
        VectorRoutingTableEntry en = table_.remove(addr);

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
    public synchronized Collection<VectorRoutingTableEntry> getEntries() {
        return table_.values();
    }

    /** Return the interface on which to send a packet to a given address
        or null if not known */
    @Override
    public synchronized NetIF getInterface(Address addr) {

        if (addr == null) {
            return null;
        }
        VectorRoutingTableEntry e = table_.get(addr);

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
        Logger.getLogger("log").logln(USR.STDOUT, "VectorRoutingTable: ADD LOCAL NET IF "+inter.getAddress());
        //Logger.getLogger("log").logln(USR.ERROR, "VectorRoutingTable: addNetIF: table before = " + this);

        Address a = inter.getAddress();

        // this is the first interface.
        // so we get the size of an Address
        if (table_.size() == 0) {
            addressSize = a.size();
            Logger.getLogger("log").logln(USR.ERROR, "VectorRoutingTable: set addressSize = " + addressSize);
        }

        // see if the table is changed
        boolean changed1 = false;

        // If necessary add this local address to routing table
        if (table_.get(a) == null) {
            VectorRoutingTableEntry e1 = new VectorRoutingTableEntry(a, fillIntegers(0), fillNetIFs(null));
            table_.put(a, e1);
            changed1 = true;
            entrySize = e1.size();
            //Logger.getLogger("log").logln(USR.ERROR, "VectorRoutingTable: set entrySize = " + entrySize);
        }

        //System.err.println("New entry from router "+inter.getRemoteRouterAddress());

        // Note weight here is 0 because weight of inter will be added by merge
        VectorRoutingTableEntry e2 =
            new VectorRoutingTableEntry(inter.getRemoteRouterAddress(), fillIntegers(0), fillNetIFs(inter));

        boolean changed2 = mergeEntry(e2, inter, options); // Add entry for remote end

        //Logger.getLogger("log").logln(USR.ERROR, "VectorRoutingTable: addNetIF: table after = " + this);
        return changed1 || changed2;
    }

    /**
     * Merge a RoutingTable into this one.
     */
    @Override
    public synchronized boolean mergeTables(RoutingTable table2, NetIF inter, RouterOptions options) {
        // Logger.getLogger("log").logln(USR.ERROR, "MERGING TABLES");
        boolean changed = false;
        Collection<VectorRoutingTableEntry> es = 
            ((VectorRoutingTable)table2).getEntries();

        // there are no entries
        if (es == null) {
            return false;
        }

        // Check if this table is telling us to remove entries
        ArrayList<Address> toRemove = new ArrayList<Address>();

        // only process entries if inter is not null
        if (inter != null) {
            for (VectorRoutingTableEntry e : getEntries()) {
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

                    VectorRoutingTableEntry e2 = (VectorRoutingTableEntry)table2.getEntry(a);

                    // If interface can no longer reach address remove it
                    if (e2 == null) {
                        toRemove.add(a);  // flag removal and do later
                        //System.err.println ("REMOVE");
                        changed = true;
                        continue;
                    }

                    // process each topology to check costs
                    for (int t = 0; t<totalTopology; t++) {
                        // If cost has become higher add higher cost
                        int receivedCost = e2.getCost(t)+inter.getWeight();

                        if (receivedCost > e.getCost(t)) {
                            e.setCost(t, receivedCost);
                            //Logger.getLogger("log").logln(USR.ERROR, "COST CHANGE");
                            changed = true;
                        }
                    }
                }
            }

            // remove entries from this RoutingTable
            for (Address a : toRemove) {
                table_.remove(a);
            }
        }

        // Add new entries as appropriate
        for (VectorRoutingTableEntry e : 
                 ((VectorRoutingTable)table2).getEntries()) {
            if (mergeEntry(e, inter, options)) {
                changed = true;
            }
        }
        return changed;
    }

    /** Get an entry from the table */
    @Override
    public synchronized VectorRoutingTableEntry getEntry(Address a) {
        return table_.get(a);
    }

    /**
     * Merge an entry in this RoutingTable returns true if there has been
     a change
    */
    public synchronized boolean  mergeEntry(VectorRoutingTableEntry newEntry, NetIF inter, RouterOptions options) {

        if (newEntry == null) {
            //System.err.println ("NULL ENTRY");
            return false;
        }
        /* if (inter == null) {
           System.err.println("Merging entry "+newEntry+" from null");
           } else {
           System.err.println("Merging entry "+newEntry+" from "+inter+" "+inter.getClass());
           }*/


        // if the Address is our Address, do nothing
        Address addr = newEntry.getAddress();

        if (listener_.ourAddress(addr)) {
            return false;
        }


        // get the weight of the interface the RoutingTable came in on
        int weight = 0;

        if (inter != null) {
            weight = inter.getWeight();
        }

        //  Logger.getLogger("log").logln(USR.ERROR, "Weight = "+weight);

        // Can't be told more about our address

        VectorRoutingTableEntry oldEntry = table_.get(addr);

        // CASE 1 -- NO ENTRY EXISTED
        if (oldEntry == null) {

            // skip through all topologies
            Integer[] newCosts = new Integer[totalTopology];

            // process each topology to check costs
            for (int t = 0; t<totalTopology; t++) {
                // Work out new cost
                newCosts[t] = newEntry.getCost(t) + weight;
                //Logger.getLogger("log").logln(USR.ERROR, "NEW ENTRY "+addr+" cost "+newCosts);
                /* ignoring max dist at moment
                   if (newCost > options.getMaxDist()) {
                   //Logger.getLogger("log").logln(USR.ERROR, "TOO EXPENSIVE");
                   return false;

                   } else {
                   }
                */

            }
            // Create new entry
            VectorRoutingTableEntry e = new VectorRoutingTableEntry(addr, newCosts, fillNetIFs(inter));
            //Logger.getLogger("log").logln(USR.ERROR, "NEW ENTRY ADDED");
            table_.put(addr, e);
            return true;

        }

        // CASE 2 -- ENTRY EXISTED BUT WAS MORE EXPENSIVE -- CHEAPER ROUTE FOUND
        // process each topology to check costs
        boolean changed = false;

        for (int t = 0; t<totalTopology; t++) {
            int newCost = newEntry.getCost(t) + weight;

            if (oldEntry.getCost(t) > newCost) {
                //System.err.println ("CHEAPER ROUTE");
                oldEntry.setCost(t, newCost);
                oldEntry.setNetIF(t, inter);
                changed = true;
            }
        }

        if (changed) {
            return true;
        }

        // CASE 3 -- ENTRY EXISTED WAS ON THIS INTERFACE -- ROUTE GOT MORE EXPENSIVE
        changed = false;

        if (inter != null && inter.equals(oldEntry.getNetIF())) {
            for (int t = 0; t<totalTopology; t++) {
                int newCost = newEntry.getCost(t) + weight;

                if (newCost > oldEntry.getCost(t)) {
                    /* ignoring max dist at moment
                       if (newCost > options.getMaxDist()) {
                       table_.remove(addr);   // Too far, can no longer route
                       } else {
                       }
                    */

                    oldEntry.setCost(t, newCost);
                    changed = true;
                    //System.err.println ("MORE EXPENSIVE ROUTE BUT SAME INTERFACE");
                }
            }

            if (changed) {
                return true;
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

        for (VectorRoutingTableEntry e : getEntries()) {
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
        fillIntegers(weight);

        // TODO: patch up vector of weights for specified NetIF

        return false;
    }


    /**
     * Fill an Integer [] with a value
     */
    private Integer[] fillIntegers(Integer i) {
        Integer [] array = new Integer[totalTopology];

        for (int t = 0; t < totalTopology; t++) {
            array[t] = i;
        }

        return array;
    }

    /**
     * Fill  an array of NetIFs
     */
    private NetIF[] fillNetIFs(NetIF n) {
        NetIF[] array = new NetIF[totalTopology];

        for (int t = 0; t < totalTopology; t++) {
            array[t] = n;
        }

        return array;
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

        for (VectorRoutingTableEntry e : getEntries()) {
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

        Collection<VectorRoutingTableEntry> rtes = getEntries();

        for (VectorRoutingTableEntry e : rtes) {
            table.append(e.toString());
            table.append("\n");
        }
        String s = table.toString();
        //Logger.getLogger("log").logln(USR.ERROR, "VectorRoutingTable is:\n"+s);
        return s;
    }

}
