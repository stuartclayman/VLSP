package usr.router;

import java.util.List;
import java.util.ArrayList;
import usr.net.*;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric implements RouterFabric, Runnable {
    // The Router this is fabric for
    Router router;

    // The RoutingTable
    RoutingTable table_= null;

    // A List of RouterPorts
    ArrayList<RouterPort> ports;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    // how many millis to wait between port checks
    int checkTime = 60000;

    /**
     * Construct a SimpleRouterFabric.
     */
    public SimpleRouterFabric(Router router) {
        this.router = router;
        table_= new RoutingTable();
        int limit = 32;
        ports = new ArrayList<RouterPort>(limit);
        for (int p=0; p < limit; p++) {
            resetPort(p);
        }
    }

    /**
     * Start me up.
     */
    public boolean start() {
        System.out.println(leadin() + "start");

        // start my own thread
        myThread = new Thread(this);
        running = true;
        myThread.start();

        return true;
    }


    /**
     * Stop the RouterController.
     */
    public boolean stop() {
        System.out.println(leadin() + "stop");

        // stop my own thread
        running = false;
        myThread.interrupt();

        // wait for myself
        waitFor();

        return true;
    }

    /**
     * The main thread loop.
     * It occasionally checks to see if the
     * NetIFs plugged into the ports are alive.
     */
    public void run() {

        while (running) {
            try {
                // sleep a bit
                Thread.sleep(checkTime);

                // visit each port
                int limit = ports.size();
                for (int p = 0;  p < limit; p++) {
                    RouterPort port = ports.get(p);

                    // check if port is plugged in
                    if (port.equals(RouterPort.EMPTY)) {
                        continue;
                    } else {
                        queryPort(port);
                    }
                }
                
            } catch (InterruptedException ie) {
                //System.err.println(leadin() + "SimpleRouterFabric: interrupt " + ie);
            }
        }

        // notify we have reached the end of this thread
        theEnd();
    }

    private void queryPort (RouterPort p)
    {
    
        NetIF net= p.getNetIF();
        Datagram dg= null;
        while (true) {
            dg= net.readDatagram();
            if (dg == null)
              return;           
            System.err.println("Woo hoo -- read datagram!");
        }
    }
    /**
     * Wait for this thread.
     */
    private synchronized void waitFor() {
        // System.out.println(leadin() + "waitFor");
        try {
            wait();
        } catch (InterruptedException ie) {
        }
    }

    /**
     * Notify this thread.
     */
    private synchronized void theEnd() {
        // System.out.println(leadin() + "theEnd");
        notify();
    }



    /**
     * Add a Network Interface to this Router.
     */
    public RouterPort addNetIF(NetIF netIF) {
        int nextFree = findNextFreePort();

        RouterPort rp = new RouterPort(nextFree, netIF);

        ports.set(nextFree, rp);

        System.out.println(leadin() + "plugged NetIF: " + netIF + " into port " + nextFree);
        table_.addNetIF(netIF);
        return rp;
    }

    /**
     * Remove a Network Interface from this Router.
     */
    public boolean removeNetIF(NetIF netIF) {
        // find port associated with netIF
        RouterPort port = findNetIF(netIF);

        if (port != null) {
            // disconnect netIF from port
            resetPort(port.getPortNo());
            netIF.close();
            return true;
        } else {
            // didn't find netIF in any RouterPort
            return false;
        }
    }

    /**
     * Get port N.
     */
    public RouterPort getPort(int p) {
        return ports.get(p);
    }

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    public List<RouterPort> listPorts() {
        return ports;
    }

    /**
     * Close ports.
     */
    public void closePorts() {
        for (RouterPort port : ports) {
            closePort(port);
        }
    }

    /**
     * Close port.
     */
    public void closePort(RouterPort port) {
        if (port.equals(RouterPort.EMPTY)) {
            // nothing to do
        } else {
            System.out.println(leadin() + "closing port " + port);
            
            NetIF netIF = port.getNetIF();
            netIF.close();
        }
    }



    /*
     * Port processing
     */

    /**
     * Reset a port
     */
    void resetPort(int p) {
        ports.add(p, RouterPort.EMPTY);
    }
    
    /**
     * Return the routing table 
     */
    public RoutingTable getRoutingTable() {
        return table_;
    }

    /**
     * Find the port a NetIF is in.
     * Skip through all ports to find a NetIF
     * @return null if a NetIF is not found.
     */
    RouterPort findNetIF(NetIF netIF) {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            RouterPort port = ports.get(p);
            if (port.getNetIF().equals(netIF)) {
                return port;
            }
        }

        return null;
    }
    
    /** Find the netIF which connects to a given end host 
      @return null if none exists*/
    
    public NetIF findNetIF(String endHostName) {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            RouterPort port = ports.get(p);
            if (port.getNetIF().getRemoteRouterName().equals(endHostName)) {
                return port.getNetIF();
            }
        }
        return null;
    }
    
    /**
     * Find the next free port to use.
     * Start at port 0 and work way up.
     */
    int findNextFreePort() {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            if (ports.get(p).equals(RouterPort.EMPTY)) {
                return p;
            }
        }

        // if we get here the ports are all full
        // so make more
        ports.ensureCapacity(limit + 8);
        for (int p = limit; p < (limit + 8); p++) {
            resetPort(p);
        }

        return limit;
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RF = "RF: ";
        RouterController controller = router.getRouterController();

        return controller.getName() + " " + RF;
    }


}
