package usr.router;
import usr.net.Address;

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

    @Override
	public String toString();

}