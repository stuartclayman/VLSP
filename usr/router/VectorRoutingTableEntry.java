package usr.router;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;

import usr.net.Address;
import usr.net.AddressFactory;

/**
 * An entry in a routing table that has a vector of topologies.
 */
public class VectorRoutingTableEntry implements RoutingTableEntry {
    private Address address_;
    private Vector<Integer> costs_;
    private Vector<NetIF> interfaces_;           // NetIF of null is a local interface

    private int totalTopology = 0; // the total no of topologies
    private int topology = 0;   // the current chosen topology

    VectorRoutingTableEntry(Address addr, List<Integer> costs, List<NetIF> inters) {
        this(addr, (costs == null ? null : costs.toArray(new Integer[costs.size()])), (inters == null ? null : inters.toArray(new NetIF[inters.size()])) );
    }

    VectorRoutingTableEntry(Address addr, Integer[] costs, NetIF[] inters) {
        if (costs == null || costs.length == 0) {
            throw new Error("costs null OR length == 0");
        }

        if (inters == null || inters.length == 0) {
            throw new Error("inters null OR length == 0");
        }

        if (costs.length != inters.length) {
            throw new Error("costs length != interfaces length");
        }

        address_ = addr;

        totalTopology = costs.length;

        //System.err.println("totalTopology = " + totalTopology + " costs = " + costs.length);

        // copy costs into costs_ vector
        costs_ = new Vector<Integer>(totalTopology);
        costs_.setSize(totalTopology);

        for (int c = 0; c<totalTopology; c++) {
            setCost(c, costs[c]);
        }

        // copy interfaces into interfaces_ vector
        interfaces_ = new Vector<NetIF>(totalTopology);
        interfaces_.setSize(totalTopology);

        for (int i = 0; i<totalTopology; i++) {
            setNetIF(i, inters[i]);
        }
    }

    VectorRoutingTableEntry(int addressSize, byte [] tableEntry, NetIF inter) throws Exception {
        if (tableEntry.length < 8) {
            throw new Exception("Byte array received to construct routing table too short");
        }
        ByteBuffer wrapper = ByteBuffer.wrap(tableEntry);

        // get correct no of bytes for an address
        int totalLength = tableEntry.length;
        int costStart = addressSize;

        // work out the number of topologies
        totalTopology = (totalLength - addressSize) / 4;


        //System.err.println("VectorRoutingTableEntry: tableEntry size = " + totalLength +
        //                   " costStart = " + costStart +
        //                   " totalTopology = " + totalTopology);

        // suck out the address
        byte[] addr = new byte[costStart];
        wrapper.get(addr);
        address_ = AddressFactory.newAddress(addr);

        // now get the cost
        wrapper.position(costStart);

        // copy costs into costs_ vector
        costs_ = new Vector<Integer>(totalTopology);
        costs_.setSize(totalTopology);

        // copy interfaces into interfaces_ vector
        interfaces_ = new Vector<NetIF>(totalTopology);
        interfaces_.setSize(totalTopology);

        for (int t = 0; t<totalTopology; t++) {
            setCost(t, wrapper.getInt());
            setNetIF(t, inter);
        }

        //System.err.println("NEW ENTRY CREATED "+toString());
    }

    /**
     * Get the current topology we are using
     */
    public int getTopology() {
        return topology;
    }

    /**
     * Set the current topology we are using
     */
    public void setTopology(int t) {
        if (t >= totalTopology) {
            throw new Error("Cannot set topology to " + t + ". Max is " + (totalTopology-1));
        } else {
            topology = t;
        }
    }

    @Override
	public Address getAddress() {
        return address_;
    }

    @Override
	public NetIF getNetIF() {
        return interfaces_.get(topology);
    }

    public NetIF getNetIF(int topol) {
        return interfaces_.get(topol);
    }

    /** Setter function for network interface */
    public void setNetIF(int topol, NetIF i) {
        interfaces_.set(topol, i);
    }

    @Override
	public int getCost() {
        return costs_.get(topology);
    }

    public int getCost(int topol) {
        return costs_.get(topol);
    }

    void setCost(int topol, int cost) {
        costs_.set(topol, cost);
    }

    /**
     * Get an Address as String representation
     */
    String addressAsString(Address addr) {
        //return addr.asTransmitForm();
        return addr.toString();
    }

    /**
     * The size in bytes of a RoutingTableEntry.
     */
    @Override
	public int size() {
        // the size of the address, plus 4 for the cost of each topology entry
        return address_.asByteArray().length + (4 * totalTopology);
    }

    /**
     * Transform as VectorRoutingTableEntry into a byte[]
     */
    public byte [] toBytes() {
        byte [] bytes = new byte[size()];
        ByteBuffer b = ByteBuffer.wrap(bytes);
        // copy in the address
        b.put(address_.asByteArray());

        // copy in all costs, 1 per topology
        for (int t = 0; t<totalTopology; t++) {
            b.putInt(costs_.get(t));
        }

        return bytes;
    }

    /**
     * SHow only data transmitted
     */
    @Override
	public String showTransmitted() {
        String entry = "[ ";

        entry += addressAsString(address_);

        for (int t = 0; t<totalTopology; t++) {
            entry += " W(" + costs_.get(t) + ") ";
        }

        entry += " ]";

        return entry;

    }

    /** Entry represented as string */
    @Override
	public String toString() {
        String entry = "[ ";

        if (interfaces_ == null) {
            entry += addressAsString(address_) + " W(0) IF: localhost";
        } else {
            entry += addressAsString(address_);
        }

        for (int t = 0; t< totalTopology; t++) {
            entry += " W(" + costs_.get(t) + ") ";

            NetIF inter_ = interfaces_.get(t);

            if (inter_ == null) {
                entry += "IF: localhost";
            } else {
                entry += "IF: " + ("if" + portNo(inter_))  + " => " + addressAsString(inter_.getRemoteRouterAddress());
            }
        }


        entry += " ]";
        //Logger.getLogger("log").logln(USR.ERROR, "ENTRY: "+entry);
        return entry;
    }

    private String portNo(NetIF inter) {
        if (inter.getRouterPort() == null) {
            return "_temp";
        } else {
            return Integer.toString(inter.getRouterPort().getPortNo());
        }
    }

}
