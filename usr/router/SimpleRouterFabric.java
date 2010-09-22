package usr.router;

import java.util.List;
import java.util.ArrayList;
import usr.net.*;
import usr.protocol.Protocol;
import java.nio.ByteBuffer;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric implements RouterFabric, NetIFListener, Runnable {
    // The Router this is fabric for
    Router router;

 

    // The RoutingTable
    SimpleRoutingTable table_= null;

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

        
        // tell the NetIF, this is its listener
        netIF.setNetIFListener(this);
        
        // add this to the RoutingTable
        if (table_.addNetIF(netIF)) {
            sendToOtherInterfaces(netIF);
        }
        return rp;
    }
    
    /** Send routing table to all other interfaces apart from inter*/
    synchronized void sendToOtherInterfaces(NetIF inter) 
      
    {
        List <NetIF> l= listNetIF();
        if (listNetIF() == null) 
            return;
        for (NetIF i: listNetIF()) {
            if (! i.equals(inter)) {
                System.out.println(leadin()+"Sending routes to other interface "+i);
                i.sendRoutingTable(table_.toString(),false);
            }
        }
    }

    /**
     * Remove a Network Interface from this Router.
     */
    public synchronized boolean removeNetIF(NetIF netIF) {
        // find port associated with netIF
        RouterPort port = findNetIF(netIF);

        if (port != null) {
            // disconnect netIF from port
            table_.removeNetIF(netIF);
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
        Datagram datagram= netIF.readDatagram();
        if (datagram == null)
            return false;
        //System.err.println("GOT DATAGRAM");

        if (ourAddress(datagram.getDstAddress())) {
            //System.err.println("OUR DATAGRAM");
            receiveDatagram(datagram,netIF);
        } else {
            //System.err.println("FORWARDING DATAGRAM");
            forwardDatagram(datagram);
        }
        return true;
    }
    
    /** Is this datagram for us */
    public synchronized boolean ourAddress(Address addr)
    {
        if (addr == null )
            return true;
        if (addr.equals(router.getAddress()))
            return true;
        for (NetIF n: listNetIF()) {
           if (addr.equals(n.getAddress()))
               return true;
        }
        return false;
    }
    
    /** Datagram which has arrived is ours */
    synchronized void  receiveDatagram(Datagram datagram, NetIF netIF) {
        // TODO check if datagram belongs here or must be forwarded 
        if (datagram.getProtocol() == Protocol.CONTROL) {
            processControlDatagram(datagram, netIF);
        } else {
            processDatagram(datagram, netIF);
        }
    }

    /** Forward a datagram */
    synchronized void forwardDatagram(Datagram dg)
    {
        Address addr= dg.getDstAddress();
        
        NetIF inter= table_.getInterface(addr);
        if (inter == null) {  // Routing table returns no interface
            
            if (ourAddress(addr)) {
                receiveDatagram(dg, null);
            }
            System.out.println(leadin() + " no route to "+addr);
        } else {
            System.out.println(leadin() + " datagram forwarding datagram to "+addr);
            inter.sendDatagram(dg);
        }
    }
    
    synchronized  void  processDatagram(Datagram dg, NetIF netIF) {
        System.err.println("GOT ORDINARY DATAGRAM");
        return;
    }

     /**
     * Process a control datagram
     */
    synchronized boolean  processControlDatagram(Datagram dg,NetIF netIF) {
        System.out.println(leadin()+"TCPNetIF: <- Control Datagram " + dg);
        // System.err.println("GOT CONTROL DATAGRAM");
        byte[] payload = dg.getPayload();
        if (payload.length == 0) {
            System.err.println("GOT LENGTH ZERO DATAGRAM");
            return true;
        }
        byte controlChar= payload[0];
        System.out.println(leadin()+"TCPNetIF: <- Control Datagram type "+
          (char)controlChar + " data "+ dg);
        //System.err.println("RECEIVED DATAGRAM CONTROL TYPE "+(char)controlChar);
        String data= new String(payload,1,payload.length-1);

        if (controlChar == 'C') {
            netIF.setRemoteClose(true);
            netIF.remoteClose();
            return true;
        }
        if (controlChar == 'T') {
            receiveRoutingTable(data,netIF);
            return true;
        }
        if (controlChar == 'R') {
            receiveRoutingTable(data,netIF);
            boolean sent = netIF.sendRoutingTable(table_.toString(),false);
            
            if (sent) {
                System.out.println(leadin() + "Send routing table SUCCESS");
            } else {
                System.out.println(leadin() + "Send routing table FAILED");
            }
            return true;
        }
        if (controlChar == 'E') {
            System.out.println(leadin()+ "Received echo");
            return true;
        }
        if (controlChar == 'P') {
            System.out.println(leadin()+ "Received ping");
            return pingResponse(dg);
        }
        System.err.println(leadin()+ "Received unknown control packet type "+
          (char)controlChar);

        return false;

    }

    /** Respond to a ping with an echo */
    synchronized boolean pingResponse(Datagram dg)
    {
       
        GIDAddress dst= (GIDAddress)dg.getSrcAddress();
        System.out.println(leadin()+"Responding to ping with echo to "+dst);
        int id= dst.getGlobalID();
        return echo(id);
    }

    /** Routing table received via netIF */
    synchronized void receiveRoutingTable(String tab, NetIF netIF)
    {   

        System.out.println(leadin()+"Received routing table");

        SimpleRoutingTable t;
        try {
            t= new SimpleRoutingTable(tab,netIF);
        } catch (Exception e) {
            System.err.println(leadin()+"Received unreadable routing table");
            System.err.println(leadin()+tab);
            System.err.println(e.getMessage());
            return;
        }
        System.out.println(leadin()+ " merging routing table received on "+netIF);
        if (table_.mergeTables(t,netIF)) {
            sendToOtherInterfaces(netIF);
        }
    }



    /**
     * A NetIF is closing.
     */
    public synchronized boolean netIFClosing(NetIF netIF) {
        System.out.println(leadin() + "Remote close from " + netIF);

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
    
    /** Ping command received */
    public synchronized boolean ping (int id) {
        GIDAddress dst= new GIDAddress(id);   
        GIDAddress src= router.getAddress();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("P".getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
        datagram.setSrcAddress(src);
        datagram.setDstAddress(dst);
        forwardDatagram(datagram);
        return true;
    }
    
    /** Echo command received */
    public synchronized boolean echo (int id) {
        GIDAddress dst= new GIDAddress(id);
        GIDAddress src= router.getAddress();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("E".getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
        datagram.setSrcAddress(src);
        datagram.setDstAddress(dst);
        forwardDatagram(datagram);
        return true;
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
