package usr.router;

import java.util.List;
import java.util.ArrayList;
import usr.net.*;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric implements RouterFabric, NetIFListener, Runnable {
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
        table_= new SimpleRoutingTable();
        int limit = 32;
        ports = new ArrayList<RouterPort>(limit);
        for (int p=0; p < limit; p++) {
            setupPort(p);
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

        // close fabric ports
        closePorts();

        // stop my own thread
        running = false;
        myThread.interrupt();


        // wait for myself
        try {
            myThread.join();
        } catch (InterruptedException ie) {
            // System.err.println("SimpleRouterFabric: stop - InterruptedException for myThread join on " + myThread);
        }

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
                        // get some port info
                    }
                }
                
            } catch (InterruptedException ie) {
                //System.err.println(leadin() + "SimpleRouterFabric: interrupt " + ie);
            }
        }
    }

    /**
     * Add a Network Interface to this Router.
     */
    public synchronized RouterPort addNetIF(NetIF netIF) {
        int nextFree = findNextFreePort();

        RouterPort rp = new RouterPort(nextFree, netIF);

        ports.set(nextFree, rp);

        System.out.println(leadin() + "plugged NetIF: " + netIF + " into port " + nextFree);

        // add this to the RoutingTable
        table_.addNetIF(netIF);

        // tell the NetIF, this is its listener
        netIF.setNetIFListener(this);

        return rp;
    }

    /**
     * Remove a Network Interface from this Router.
     */
    public synchronized boolean removeNetIF(NetIF netIF) {
        // find port associated with netIF
        RouterPort port = findNetIF(netIF);

        if (port != null) {
            // disconnect netIF from port
            closePort(port);
            resetPort(port.getPortNo());
            return true;
        } else {
            // didn't find netIF in any RouterPort
            return false;
        }
    }

    /**
     * Get port N.
     */
    public synchronized RouterPort getPort(int p) {
        return ports.get(p);
    }

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    public synchronized List<RouterPort> listPorts() {
        return ports;
    }

    /**
     * Close ports.
     */
    public synchronized void closePorts() {
        for (RouterPort port : ports) {
            if (port.equals(RouterPort.EMPTY)) {
                continue;
            } else {
                closePort(port);
                resetPort(port.getPortNo());
            }
        }
    }

    /**
     * Close port.
     */
    public synchronized void closePort(RouterPort port) {
        if (port.equals(RouterPort.EMPTY)) {
            // nothing to do
        } else {
            System.out.println(leadin() + "closing port " + port);
            
            NetIF netIF = port.getNetIF();

            if (!netIF.isClosed()) {
                netIF.close();
                System.out.println(leadin() + "closed port " + port);
            } else {
                System.out.println(leadin() + "ALREADY closed port " + port);
            }
        }
    }

    /**
     * A NetIF has a datagram.
     */
    public synchronized boolean datagramArrived(NetIF netIF) {
        System.err.println(leadin() + "Datagram Arrived on " + netIF);
        return true;
    }

    /**
     * A NetIF is closing.
     */
    public synchronized boolean netIFClosing(NetIF netIF) {
        System.err.println(leadin() + "Remote close from " + netIF);

        if (!netIF.isClosed()) {

            boolean didit = removeNetIF(netIF);

            return didit;
        } else {
            return false;
        }
    }


    /*
     * Port processing
     */

    /**
     * Setup a port
     */
    synchronized void setupPort(int p) {
        ports.add(p, RouterPort.EMPTY);
    }
    
    /**
     * Reset a port
     */
    synchronized void resetPort(int p) {
        ports.set(p, RouterPort.EMPTY);
    }
    
    /**
     * Return the routing table 
     */
    public synchronized RoutingTable getRoutingTable() {
        return table_;
    }

    /**
     * Find the port a NetIF is in.
     * Skip through all ports to find a NetIF
     * @return null if a NetIF is not found.
     */
    synchronized RouterPort findNetIF(NetIF netIF) {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            RouterPort port = ports.get(p);

            if (port.equals(RouterPort.EMPTY)) {
                continue;
            } else if (port.getNetIF().equals(netIF)) {
                return port;
            } else {
                ;
            }
        }

        return null;
    }
    
     /** 
     * Get a list of all connected Network Interfaces
     */
    public synchronized List<NetIF> listNetIF() {
        ArrayList<NetIF> list = new ArrayList<NetIF>();
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            RouterPort port = ports.get(p);

            if (port.equals(RouterPort.EMPTY)) {
                continue;
            } 
            list.add(port.getNetIF());
        }

        return list;
    }
    
    /** Find the netIF which connects to a given end host 
      @return null if none exists*/
    
    public synchronized NetIF findNetIF(String endHostName) {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            RouterPort port = ports.get(p);

            if (port.equals(RouterPort.EMPTY)) {
                continue;
            } else if (port.getNetIF().getRemoteRouterName().equals(endHostName)) {
                return port.getNetIF();
            } else {
                ;
            }
        }
        return null;
    }
    
    /**
     * Find the next free port to use.
     * Start at port 0 and work way up.
     */
    synchronized int findNextFreePort() {
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
            setupPort(p);
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
