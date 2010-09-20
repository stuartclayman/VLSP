package usr.router;

import java.util.*;
import usr.net.*;

public interface RoutingTable {


public int size();

/** Get all entries from the routing table*/
public Collection<? extends RoutingTableEntry> getEntries();

public void addNetIF(NetIF inter);

/** Add an address which is associated with this router */
public void removeNetIF(NetIF inter);

public String toString();

/** Add an address which is associated with this router */
public void addMyAddress(Address a);

}
