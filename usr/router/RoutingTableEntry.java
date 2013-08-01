package usr.router;
import usr.net.Address;
import java.util.*;
import usr.logging.*;

public interface RoutingTableEntry {

    public NetIF getNetIF();

    public Address getAddress();

    public int getCost();

    /**
     * The size, in bytes, of a RoutingTableEntry.
     */
    public int size();

    /**
     * SHow only data transmitted
     */
    public String showTransmitted();

    public String toString();

}