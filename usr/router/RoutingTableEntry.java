package usr.router;
import usr.net.Address;
import java.util.*;
import usr.logging.*;

public interface RoutingTableEntry {

    public NetIF getNetIF();

    public Address getAddress();

    public int getCost();

    public String toString();

}
