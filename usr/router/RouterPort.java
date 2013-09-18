package usr.router;

/**
   import usr.logging.*;
 * A port on a Router
 */
public class RouterPort {
    final int portNo;
    final NetIF netIF;

    public static final RouterPort EMPTY = new RouterPort(-1, null);

    /**
     * Construct a RouterPort, with a specied no.
     */
    public RouterPort(int no, NetIF netIF) {
        portNo = no;
        this.netIF = netIF;
    }

    /**
     * Get the port no
     */
    public int getPortNo() {
        return portNo;
    }

    /**
     * Get the NetIF in this port.
     */
    public NetIF getNetIF() {
        return netIF;
    }

    /**
     * Get name
     */
    public String getName() {
        return "port" + portNo + " " + netIF;
    }

    /**
     * To String
     */
    @Override
	public String toString() {
        return getName() + ": " + netIF;
    }

}