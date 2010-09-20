package usr.router;

import java.util.*;

public interface RoutingTable {


public int size();

public Collection<RoutingTableEntry> getEntries();

public void addNetIF(NetIF inter);

public void removeNetIF(NetIF inter);

public String toString();

}
