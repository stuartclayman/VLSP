package usr.router;

import java.util.*;
import usr.logging.*;
import usr.net.*;

public interface RoutingTable
{
/**
 * The size of the RoutingTable.
 */
public int size();

/** Get all entries from the routing table*/
public Collection<? extends RoutingTableEntry> getEntries();

/** Get an entry from the table */
public RoutingTableEntry getEntry(Address a);

/**
 * Merge a RoutingTable into this one.
 */
public boolean mergeTables(RoutingTable table2,
    NetIF inter,
    RouterOptions options);

/** A new network interface arrives -- add to
 * routing table if necessary return true if change was made */
public boolean addNetIF(NetIF inter, RouterOptions o);

/** Removes a network interface from a router returns true if
 * routing table has changed*/
public boolean removeNetIF(NetIF inter);

/** Set the NetIFListener */
public void setListener(NetIFListener l);

/** Return the interface on which to send a packet to a given address
 * or null if not known */
public NetIF getInterface(Address addr);

/**
 * SHow only data transmitted
 */
public String showTransmitted();

public String toString();

public byte[] toBytes();
}