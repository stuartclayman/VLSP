package usr.router;

import java.util.List;
import java.util.ArrayList;
import usr.net.*;
import usr.protocol.Protocol;
import java.nio.ByteBuffer;
import java.lang.*;
import java.util.*;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric implements RouterFabric, NetIFListener, Runnable {
    // The Router this is fabric for
    Router router;
    RouterOptions options_;

    // A List of RouterPorts
    ArrayList<RouterPort> ports;

    // The localNetIF
    NetIF localNetIF = null;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    // the count of the no of Datagrams
    int datagramCount = 0;

    // The RoutingTable
    SimpleRoutingTable table_= null;

    // routing table info
    long nextUpdateTime_= 0;
    NetIF nextUpdateIF_= null;

    HashMap <NetIF, Long> lastTableUpdateTime_;
    HashMap <NetIF, Long> nextTableUpdateTime_;
    
    /**
     * Construct a SimpleRouterFabric.
     */
    public SimpleRouterFabric(Router router, RouterOptions opt) {
        this.router = router;
        options_= opt;
        table_= new SimpleRoutingTable();
        int limit = 32;
        ports = new ArrayList<RouterPort>(limit);
        
        for (int p=0; p < limit; p++) {
            setupPort(p);
        }

        localNetIF = null;

        lastTableUpdateTime_= new HashMap <NetIF, Long>();
        nextTableUpdateTime_= new HashMap <NetIF, Long>();
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
       long now=  System.currentTimeMillis();
       nextUpdateTime_= now+options_.getMaxCheckTime();
       while (running) {
            calcNextTableSendTime();
            now= System.currentTimeMillis();
            //System.err.println("TIME: "+now);
            if (nextUpdateTime_ <= now) {
                //System.err.println("Sending table");
                sendNextTable();
                continue;
            }
            if (Thread.interrupted()) {
                continue;
            }
            //System.err.println("Waiting Until: "+nextEventTime);
            waitUntil(nextUpdateTime_);
        }
    }


     /**
     * Wait until a specified absolute time is milliseconds.
     */
    public synchronized void waitUntil(long time){
        long now = System.currentTimeMillis();

        if (time <= now)
            return;
        try {
            long timeout = time - now;
            wait(timeout);
        } catch(InterruptedException e){
            //System.err.println("Interrupt");

        }
    }
    
    /** Calculate when the next table send event is */
    public synchronized void calcNextTableSendTime() {
        long now= System.currentTimeMillis();
        
        nextUpdateIF_= null;
        for (NetIF n: listNetIF()) {
            
            Long next= nextTableUpdateTime_.get(n);
            if (next == null) {
                System.err.println(leadin()+"NetIF not in nextTableTime");
                continue;
            }
            //System.err.println("Considering update from "+n+" at time "+next);
            if (next < nextUpdateTime_) {
                //System.err.println("Next update interface is now "+n);
                nextUpdateTime_= next;
                nextUpdateIF_= n;
            }
        }
        //System.err.println("Next event at "+nextUpdateTime_+" from "+nextUpdateIF_);

    }
    
    /** Now send a routing table */
    public synchronized void sendNextTable() {
        NetIF n= nextUpdateIF_;
        long now= System.currentTimeMillis();
        if (n == null) {
            //System.err.println("No table to send");
            nextUpdateTime_= now+options_.getMaxCheckTime();
            calcNextTableSendTime();
            return;
        }
        //System.err.println("Sending table for "+n);
        n.sendRoutingTable(table_.toString());
        
        lastTableUpdateTime_.put(n,now);
        nextTableUpdateTime_.put(n,now+options_.getMaxNetIFUpdateTime());
        nextUpdateIF_= null;
    }
      
    /** NetIF wants to send a routing Request */
    
    public synchronized void queueRoutingRequest(NetIF netIF) {
        long now= System.currentTimeMillis();
        Long last= lastTableUpdateTime_.get(netIF);
        Long curr= nextTableUpdateTime_.get(netIF);
        if (last == null || curr == null) {
            System.err.println(leadin()+"NetIF not in nextTableTime");
            return;
        }
        Long next= last + options_.getMinNetIFUpdateTime();
        
        if (next >= curr) // Already updating at this time or earlier 
            return;
        lastTableUpdateTime_.put(netIF,next);
        if (next <= now) {
            myThread.interrupt();
        }
        
    }  
      
    /**
     * Add a Network Interface to this Router.
     */
    public synchronized RouterPort addNetIF(NetIF netIF) {
        GIDAddress address = (GIDAddress)netIF.getAddress();
        boolean localPort= (address.getGlobalID() == 0);
        RouterPort rp= null;
        if (!localPort) {
            int nextFree = findNextFreePort();

            rp = new RouterPort(nextFree, netIF);

            ports.set(nextFree, rp);

            System.out.println(leadin() + "plugged NetIF: " + netIF + " into port " + nextFree);
        }
        
        // tell the NetIF, this is its listener
        netIF.setNetIFListener(this);
        if (localPort) {
            if (localNetIF != null) {
                System.err.println(leadin() + "Attempt to create second local multiplex port");
            }
            localNetIF= netIF;
            return null;
        }
        Long next= System.currentTimeMillis();
        lastTableUpdateTime_.put(netIF,new Long(0));
        nextTableUpdateTime_.put(netIF,next);
        System.err.println("REQUEST NEW ROUTING TABLE UPDATE NOW");
        queueRoutingRequest(netIF);
        
        // add this to the RoutingTable
        if (table_.addNetIF(netIF)) {
            sendToOtherInterfaces(netIF);
            
        }

        myThread.interrupt();

        return rp;
    }
    
    /**
     * Remove a Network Interface from this Router.
     */
    public synchronized boolean removeNetIF(NetIF netIF) {
        // find port associated with netIF
        
        GIDAddress address = (GIDAddress)netIF.getAddress();
        boolean localPort= (address.getGlobalID() == 0);
        if (localPort) {
            localNetIF= null;
            return true;
        }
        RouterPort port = findNetIF(netIF);

        if (port != null) {
            // disconnect netIF from port
            
            closePort(port);
            resetPort(port.getPortNo());

            // Remove table update times
            lastTableUpdateTime_.remove(netIF);
            nextTableUpdateTime_.remove(netIF);
            myThread.interrupt();
            if (table_.removeNetIF(netIF)) {
                sendToOtherInterfaces(null);
            }

            return true;
        } else {
            // didn't find netIF in any RouterPort
            return false;
        }
    }

    /** Send routing table to all other interfaces apart from inter*/
    synchronized void sendToOtherInterfaces(NetIF inter) 
      
    {
        List <NetIF> l= listNetIF();

        if (l == null)  {
            return;
        } else {
            for (NetIF i: l) {
                GIDAddress address = (GIDAddress)i.getAddress();
                if (address.getGlobalID() == 0) {
                    // dont queue RoutingTable for 0
                } else if (! i.equals(inter)) {
                    System.out.println(leadin()+"Queuing routes to other interface "+i);
                    queueRoutingRequest(i);

                } else {
                    // dont send out RoutingTable
                }
            }
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
        if (localNetIF != null) {
            localNetIF.close();
        }
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

        datagramCount++;

        if (datagram == null)
            return false;
        
        //System.err.println(leadin() + datagramCount + " GOT DATAGRAM from " + netIF.getRemoteRouterAddress() + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        if (ourAddress(datagram.getDstAddress())) {
            //System.err.println("OUR DATAGRAM");
            receiveOurDatagram(datagram,netIF);
        } else {
            //System.err.println("FORWARDING DATAGRAM");
            forwardDatagram(datagram);
        }
        return true;
    }
    
    /** Is this datagram for us */
    public synchronized boolean ourAddress(Address addr)
    {
        //System.out.println("DATAGRAM WITH NULL ADDRESS");
        if (addr == null )
            return true;
        if (addr.equals(router.getAddress()))
            return true;
        for (NetIF n: listNetIF()) { 
           if (addr.equals(n.getAddress())) {
               System.err.println ("ADDRESS MATCH "+addr+ " OUR INTERFACE "+n.getAddress());
               return true;
           }
        }
        return false;
    }
    
    /** Datagram which has arrived is ours */
    synchronized void  receiveOurDatagram(Datagram datagram, NetIF netIF) {
         
        if (datagram.getProtocol() == Protocol.CONTROL) {
            processControlDatagram(datagram, netIF);
        } else {
            processDatagram(datagram, netIF);
        }
    }

    /** Send a datagram */
    synchronized void sendDatagram(Datagram datagram)
    {
        System.err.println(leadin() + " SEND = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        Address addr= datagram.getDstAddress();
        //System.err.println("Forwarding datagram with source "+dg.getSrcAddress());
        NetIF inter= table_.getInterface(addr);
        if (inter == null) {  // Routing table returns no interface
            
            if (ourAddress(addr)) {
                receiveOurDatagram(datagram, null);
                return;
            }
            System.out.println(leadin() + " no route to "+addr);
            // TODO -- THIS MIGHT BE DEALT WITH BETTER
        } else {
            //System.out.println(leadin() + " sending datagram to "+addr);
            inter.sendDatagram(datagram);
        }
    }

    /** Forward a datagram */
    synchronized void forwardDatagram(Datagram datagram)
    {
        System.err.println(leadin() + " FORWARD = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        Address addr= datagram.getDstAddress();
        //System.err.println("Forwarding datagram with source "+dg.getSrcAddress());
        NetIF inter= table_.getInterface(addr);

        if (inter == null) {  // Routing table returns no interface
            
            if (ourAddress(addr)) {
                receiveOurDatagram(datagram, null);
                return;
            }
            System.out.println(leadin() + " no route to "+addr);
        } else {
            if (datagram.TTLReduce()) {
                inter.forwardDatagram(datagram);
            } else {
                sendTTLExpired(datagram);
            }
        }
    }
    
    /** Return a TTL expired datagram unless this is a TTL expired datagram */
    synchronized void sendTTLExpired(Datagram dg)
    {
        // Can't return datagram with no source
        if (dg.getSrcAddress() == null)  
            return;
            
        // Don't return TTL expired datagram.
        if (dg.getProtocol() == Protocol.CONTROL) {
            byte []payload= dg.getPayload();
            if (payload.length > 0 && (char)payload[0] == 'X')
                return;
        }
        // OK -- send TTL expired datagram
        Address dst= dg.getSrcAddress();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("X".getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);

        datagram.setDstAddress(dst);
        sendDatagram(datagram);
    }
    
    /**
     * Process a datagram.
     */
    synchronized  void  processDatagram(Datagram datagram, NetIF netIF) {
        System.err.println(leadin() + datagramCount + " GOT ORDINARY DATAGRAM from " + netIF.getRemoteRouterAddress() + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        // forward datagram if there is a local NetIF
        if (localNetIF != null) {
            localNetIF.forwardDatagram(datagram);
        }


        return;
    }

     /**
     * Process a control datagram
     */
    synchronized boolean  processControlDatagram(Datagram dg,NetIF netIF) {
        System.out.println(leadin()+"TCPNetIF: " + datagramCount + " <- Control Datagram " + dg + " from " + (netIF == null ? "self" : netIF.getRemoteRouterAddress()));



        // System.err.println("GOT CONTROL DATAGRAM");
        byte[] payload = dg.getPayload();
        if (payload.length == 0) {
            System.err.println("GOT LENGTH ZERO DATAGRAM");
            return true;
        }
        byte controlChar= payload[0];
        System.out.println(leadin()+"TCPNetIF: " + datagramCount + " <- Control Datagram type "+
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
        
        if (controlChar == 'X') {
            System.out.println(leadin()+ "Received TTL expired from "+dg.getSrcAddress()
                +":"+dg.getSrcPort());
        }
        if (controlChar == 'E') {
            System.out.println(leadin()+ "Received echo from "+dg.getSrcAddress()
                +":"+dg.getSrcPort());
            return true;
        }
        if (controlChar == 'P') {
            System.out.println(leadin()+ "Received ping from "+dg.getSrcAddress()
                +":"+dg.getSrcPort());
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
        System.out.println(leadin()+"Received routing table from " + netIF);

        SimpleRoutingTable t;
        try {
            t= new SimpleRoutingTable(tab,netIF);
        } catch (Exception e) {
            System.err.println(leadin()+"Received unreadable routing table");
            System.err.println(leadin()+tab);
            System.err.println(e.getMessage());
            return;
        }
        //System.out.println(leadin()+ " merging routing table received on "+netIF);
        boolean merged = table_.mergeTables(t,netIF);

        System.out.println(leadin()+ " merged routing table received on "+netIF);
        if (merged) {
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
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("P".getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
        datagram.setDstAddress(dst);
        sendDatagram(datagram);

        return true;
    }
    
    /** Echo command received */
    public synchronized boolean echo (int id) {
        GIDAddress dst= new GIDAddress(id);
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("E".getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);

        datagram.setDstAddress(dst);
        sendDatagram(datagram);
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
