package usr.router;

import java.util.List;
import usr.logging.*;
import java.util.ArrayList;
import usr.net.*;
import usr.protocol.Protocol;
import java.nio.ByteBuffer;
import java.lang.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric implements RouterFabric, NetIFListener, Runnable {
    // The Router this is fabric for
    Router router;
    RouterOptions options_;

    boolean theEnd=false;

    // A List of RouterPorts
    ArrayList<RouterPort> ports;
   
    LinkedBlockingQueue<DatagramHandle> datagramQueue_;
    //LinkedBlockingQueue<NetIF> netIFQueue_;
    
    // The localNetIF
    NetIF localNetIF = null;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    // are we waiting
    boolean waiting = false;

    // the count of the no of Datagrams
    int datagramCount = 0;

    // The RoutingTable
    SimpleRoutingTable table_= null;

    // routing table info

    NetIF nextUpdateIF_= null;

    HashMap <NetIF, Long> lastTableUpdateTime_;
    HashMap <NetIF, Long> nextTableUpdateTime_;

    long nextUpdateTime;
    
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
        datagramQueue_= new LinkedBlockingQueue<DatagramHandle>();
        //netIFQueue_= new LinkedBlockingQueue<NetIF>();
    }

    /**
     * Start me up.
     */
    public boolean start() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");

        // start my own thread
        running = true;
        myThread = new Thread(this);
        
        //Logger.getLogger("log").logln(USR.ERROR, "Running set to true");
        myThread.start();

        return true;
    }


    /**
     * Stop the RouterController.
     */
    public synchronized boolean stop() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");

        //Logger.getLogger("log").logln(USR.ERROR, "Closing ports");
        // close fabric ports
        closePorts();

        // stop my own thread
        //Logger.getLogger("log").logln(USR.ERROR, "Run set to false");
        running = false;

        // notify all waiting threads
        notifyAll();


       //try {
        //      if (myThread.isAlive()) 
     //       myThread.join();
     //  } catch (InterruptedException ie) {
     //       Logger.getLogger("log").logln(USR.ERROR, "SimpleRouterFabric: stop - InterruptedException for myThread join on " + myThread);
     // }

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
        long now=  System.currentTimeMillis();
        while (true) {
            synchronized (this) {
                if (running || datagramQueue_.size() > 0) {
                    if (datagramQueue_.size() > 0) {
                        //Logger.getLogger("log").logln(USR.ERROR, "Got datagram to process");
                        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "queue size " + datagramQueue_.size());
                        processDatagram();
                        continue;
                    }
                } else {
                    // not running and nothing left in queue
                    break;
                }
            }

            //Logger.getLogger("log").logln(USR.ERROR, "Running");

            now = System.currentTimeMillis();

            // dont need to do this every time, but how
            nextUpdateTime = calcNextTableSendTime();

            //Logger.getLogger("log").logln(USR.ERROR, "Got time");

            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "run TIME: "+now + " nextUpdateTime: " + nextUpdateTime_ + " diff: " + (nextUpdateTime_ - now));
            
            if (nextUpdateTime <= now) {
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Sending table");

                sendNextTable();
                continue;
            }
            
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Waiting Until: "+ nextUpdateTime);
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "run Waiting For: "+ ((float)(nextUpdateTime_-now))/1000);
            //Logger.getLogger("log").logln(USR.ERROR, "Time now "+ now);

            if (running) {
                waitUntil(nextUpdateTime);
            }
                
            //Logger.getLogger("log").logln(USR.ERROR, "Running is "+running);

        }

        theEnd();

        //Logger.getLogger("log").logln(USR.ERROR, "Exit here");
        //System.err.flush();
    }


    /**
     * Wait for this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void waitFor() {
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "waitFor");
        
        try {
            synchronized(this) {
              setTheEnd();
              notify(); //Attempt to wake "the end" process
              wait();
            }
        } catch (InterruptedException ie) {
        }
    }
    
    /**
     * Notify this thread -- DO NOT MAKE WHOLE FUNCTION synchronized
     */
    private void theEnd() {
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "theEnd");
        while (!ended()) {
            try {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"In a loop");
                wait(100);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, "The end interrupted");
            }
        }
        synchronized(this) {
            notify();
        }
    }

    synchronized void setTheEnd() {
        theEnd= true;
    }

    synchronized boolean ended() {
      return theEnd;  
    }


     /**
     * Wait until a specified absolute time is milliseconds.
     */
    synchronized void waitUntil(long time){
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Wait until " + time);
        long now = System.currentTimeMillis();

        if (time <= now)
            return;
        try {
            long timeout = time - now + 1;
            waiting = true;

            wait(timeout);

            waiting = false;
        } catch(InterruptedException e){
            //Logger.getLogger("log").logln(USR.ERROR, "Wait interrupted");
            waiting = false;
        }
    }
    
    /** Calculate when the next table send event is */
    synchronized long calcNextTableSendTime() {
        long now= System.currentTimeMillis();
        long nextUpdateTime= now+options_.getMaxCheckTime();
        nextUpdateIF_= null;
        for (NetIF n: listNetIF()) {
            
            Long next= nextTableUpdateTime_.get(n);
            if (next == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"NetIF not in nextTableTime");
                continue;
            }
            //Logger.getLogger("log").logln(USR.ERROR, "Considering update from "+n+" at time "+next);
            if (next < nextUpdateTime) {
                //Logger.getLogger("log").logln(USR.ERROR, "Next update interface is now "+n);
                nextUpdateTime= next;
                nextUpdateIF_= n;
            }
        }
        //Logger.getLogger("log").logln(USR.ERROR, "Next event at "+nextUpdateTime+" from "+nextUpdateIF_);
        return nextUpdateTime;
    }
    
    /** Now send a routing table */
    synchronized void sendNextTable() {
        NetIF n= nextUpdateIF_;
        long now= System.currentTimeMillis();
        if (n == null) {
            //Logger.getLogger("log").logln(USR.ERROR, "No table to send");
            
            return;
        }

        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + now+" sending table " + table_ + " for "+n);

        n.sendRoutingTable(table_.toString());
        
        lastTableUpdateTime_.put(n,now);
        nextTableUpdateTime_.put(n,now+options_.getMaxNetIFUpdateTime());
        //Logger.getLogger("log").logln(USR.ERROR, "Next table update time"+nextUpdateTime_);

    }
      
    /** NetIF wants to send a routing Request */
    synchronized void queueRoutingRequest(NetIF netIF) {
        long now= System.currentTimeMillis();
        Long last= lastTableUpdateTime_.get(netIF);
        Long curr= nextTableUpdateTime_.get(netIF);
        if (last == null || curr == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"NetIF not in nextTableTime");
            return;
        }
        Long next= last + options_.getMinNetIFUpdateTime();
        
        if (next >= curr) // Already updating at this time or earlier 
            return;
        nextTableUpdateTime_.put(netIF,next);
        if (next <= now) {
            notifyAll();
        }
        
    }  
      
    /**
     * Add a Network Interface to this Router.
     */
    public synchronized RouterPort addNetIF(NetIF netIF) {
        Address address = netIF.getAddress();
        boolean localPort= (address.asInteger() == 0);
        RouterPort rp= null;
        if (!localPort) {
            int nextFree = findNextFreePort();

            rp = new RouterPort(nextFree, netIF);

            ports.set(nextFree, rp);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "plugged NetIF: " + netIF + " into port " + nextFree);
        }
        
        // tell the NetIF, this is its listener
        netIF.setNetIFListener(this);
        if (localPort) {
            if (localNetIF != null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Attempt to create second local multiplex port");
            }
            localNetIF= netIF;
            return null;
        }
        Long next= System.currentTimeMillis();
        lastTableUpdateTime_.put(netIF,new Long(0));
        nextTableUpdateTime_.put(netIF,next);
        //Logger.getLogger("log").logln(USR.ERROR, "REQUEST NEW ROUTING TABLE UPDATE NOW");
        queueRoutingRequest(netIF);
        
        // add this to the RoutingTable
        if (table_.addNetIF(netIF)) {
            sendToOtherInterfaces(netIF);
            
        }

        notifyAll();

        return rp;
    }
    
    /**
     * Remove a Network Interface from this Router.
     */
    public synchronized boolean removeNetIF(NetIF netIF) {
        // find port associated with netIF
        //Logger.getLogger("log").logln(USR.ERROR, "REMOVE NETIF");
        Address address = netIF.getAddress();
        boolean localPort= (address.asInteger() == 0);
        if (localPort) {
            closeLocalNetIF();
            //Logger.getLogger("log").logln(USR.ERROR, "Removed local");
            return true;
        }
        RouterPort port = findNetIF(netIF);

        if (port != null) {
            // disconnect netIF from port
            //Logger.getLogger("log").logln(USR.ERROR, "CLOSE PORT");
            closePort(port);
            //Logger.getLogger("log").logln(USR.ERROR, "RESET PORT");
            resetPort(port.getPortNo());

            // Remove table update times
            lastTableUpdateTime_.remove(netIF);
            nextTableUpdateTime_.remove(netIF);
            notifyAll();
            if (table_.removeNetIF(netIF)) {
                sendToOtherInterfaces(netIF);
            }
            //Logger.getLogger("log").logln(USR.ERROR, "REMOVED");
            return true;
        } else {
            //Logger.getLogger("log").logln(USR.ERROR, "NOT CONNECTED TO PORT");
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
                Address address = i.getAddress();
                if (address.asInteger() == 0) {
                    // dont queue RoutingTable for 0
                } else if (! i.equals(inter)) {
                    //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Queuing routes to other interface "+i);
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
        //Logger.getLogger("log").logln(USR.ERROR, "Local NetIF close");
        closeLocalNetIF();
        
        //Logger.getLogger("log").logln(USR.ERROR, "Closing ports");
        for (int i= 0; i < ports.size(); i++) {
           // Logger.getLogger("log").logln(USR.ERROR, "Closing port "+i);
            RouterPort port= ports.get(i);
            if (port == null)
                continue;
            closePort(port);
            int pno= port.getPortNo();
            if (pno >= 0)
                resetPort(port.getPortNo());
        }
        
    }

    synchronized void closeLocalNetIF() {
        if (localNetIF != null) {
            //Logger.getLogger("log").logln(USR.ERROR, "TRYING TO CLOSE LOCALNETIF");
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Closing " + localNetIF + " stats = " + localNetIF.getStats());

            localNetIF.close();
            //Logger.getLogger("log").logln(USR.ERROR, "IT'S SHUT");
        }

        localNetIF= null;
    }

    /**
     * Close port.
     */
    public synchronized void closePort(RouterPort port) {
        if (port.equals(RouterPort.EMPTY)) {
            // nothing to do
        } else {
            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "closing port " + port);
            
            NetIF netIF = port.getNetIF();

            if (!netIF.isClosed()) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Closing " + netIF + " stats = " + netIF.getStats());

                netIF.close();

                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "closed port " + port);
            } else {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "ALREADY closed port " + port);
            }
            //Logger.getLogger("log").logln(USR.ERROR, "DONE");
        }
    }

    /**
     * A NetIF has a datagram.
     */
    public synchronized boolean datagramArrived(NetIF netIF, Datagram datagram) {
        

        datagramCount++;

        if (datagram == null)
            return false;

        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "D(" + datagramCount + ")");

        datagramQueue_.add(new DatagramHandle(datagram, netIF));
        //netIFQueue_.add(netIF);

        if (waiting) {
            myThread.interrupt();
        } 

        //notify();

        return true;
    }
   
   
    public synchronized boolean processDatagram() {
        
        if (datagramQueue_.size() == 0)
            return false;
        DatagramHandle datagramHandle =  datagramQueue_.poll();
        //NetIF netIF= netIFQueue_.poll(); 

        Datagram datagram = datagramHandle.datagram;
        NetIF netIF = datagramHandle.netIF;

        if (datagram == null) {
            return false;
        }
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + datagramCount + " GOT DATAGRAM from " + netIF.getRemoteRouterAddress() + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());
        if (ourAddress(datagram.getDstAddress())) {
            //Logger.getLogger("log").logln(USR.ERROR, "OUR DATAGRAM");
            receiveOurDatagram(datagram,netIF);
        } else {
            //Logger.getLogger("log").logln(USR.ERROR, "FORWARDING DATAGRAM");
            forwardDatagram(datagram);
        }
        return true;
    }
    
    /** Is this datagram for us */
    public synchronized boolean ourAddress(Address addr)
    {
        //Logger.getLogger("log").logln(USR.STDOUT, "DATAGRAM WITH NULL ADDRESS");
        if (addr == null )
            return true;
        if (addr.equals(router.getAddress()))
            return true;
        for (NetIF n: listNetIF()) { 
           if (addr.equals(n.getAddress())) {
               //System.err.println ("ADDRESS MATCH "+addr+ " OUR INTERFACE "+n.getAddress());
               return true;
           }
        }
        return false;
    }
    
    /** Datagram which has arrived is ours */
    void  receiveOurDatagram(Datagram datagram, NetIF netIF) {
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " receiveOurDatagram ");
         
        if (datagram.getProtocol() == Protocol.CONTROL) {
            processControlDatagram(datagram, netIF);
        } else {
            processDatagram(datagram, netIF);
        }
    }

    /** Send a datagram */
    synchronized void sendDatagram(Datagram datagram)
    {
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + " SEND = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        Address addr= datagram.getDstAddress();
        //Logger.getLogger("log").logln(USR.ERROR, "Forwarding datagram with source "+dg.getSrcAddress());
        NetIF inter= table_.getInterface(addr);
        if (inter == null) {  // Routing table returns no interface
            
            if (ourAddress(addr)) {
                receiveOurDatagram(datagram, null);
                return;
            } else {
                noRoute(datagram);
            }

        } else {
            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " sending datagram to "+addr);
            inter.sendDatagram(datagram);
        }
    }

    /** Forward a datagram */
    synchronized void forwardDatagram(Datagram datagram)
    {
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + " FORWARD = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        Address addr= datagram.getDstAddress();
        //Logger.getLogger("log").logln(USR.ERROR, "Forwarding datagram with source "+dg.getSrcAddress());
        NetIF inter= table_.getInterface(addr);

        if (inter == null) {  // Routing table returns no interface
            
            if (ourAddress(addr)) {
                receiveOurDatagram(datagram, null);
                return;
            }
            noRoute(datagram);
            
            
        } else {
            if (datagram.TTLReduce()) {
                inter.forwardDatagram(datagram);
            } else {
                sendTTLExpired(datagram);
            }
        }
    }
    
    synchronized void noRoute(Datagram dg) 
    {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + " no route to "+dg.getDstAddress());
        /** TODO -- improve this */
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
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + datagramCount + " GOT ORDINARY DATAGRAM from " + netIF.getRemoteRouterAddress() + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        // forward datagram if there is a local NetIF
        if (localNetIF != null && datagram.getDstPort() != 0) {
            localNetIF.forwardDatagram(datagram);
            return;
        }

        Logger.getLogger("log").logln(USR.ERROR, leadin() + datagramCount + " FABRIC GOT ORDINARY DATAGRAM from " + netIF.getRemoteRouterAddress() + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());


        return;
    }

     /**
     * Process a control datagram
     */
    synchronized boolean  processControlDatagram(Datagram dg,NetIF netIF) {
       

        // forward datagram if there is a local NetIF and port is not zero
        if (localNetIF != null && dg.getDstPort() != 0) {
            localNetIF.forwardDatagram(dg);
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"datagram to port "+dg.getDstPort()+" passed to LOCAL NETIF");
            
            return true;
        }
         
        // Logger.getLogger("log").logln(USR.ERROR, "GOT CONTROL DATAGRAM");
        byte[] payload = dg.getPayload();
        if (payload.length == 0) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"GOT LENGTH ZERO DATAGRAM");
            return true;
        }
        byte controlChar= payload[0];
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"TCPNetIF: " + datagramCount + " <- Control Datagram type "+
          (char)controlChar + " data "+ dg);
        //Logger.getLogger("log").logln(USR.ERROR, "RECEIVED DATAGRAM CONTROL TYPE "+(char)controlChar);
        String data= new String(payload,1,payload.length-1);

        if (controlChar == 'C') {
            netIF.remoteClose();
            return true;
        }
        if (controlChar == 'T') {
            receiveRoutingTable(data,netIF);
            return true;
        }
        
        if (controlChar == 'X') {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Received TTL expired from "+dg.getSrcAddress()
                +":"+dg.getSrcPort());
        }
        if (controlChar == 'E') {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Received echo from "+dg.getSrcAddress()
                +":"+dg.getSrcPort()+ " to "+dg.getDstAddress()+":"+
                 dg.getDstPort());
            return true;
        }
        if (controlChar == 'P') {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Received ping from "+dg.getSrcAddress()
                +":"+dg.getSrcPort());
            return pingResponse(dg);
        }
        Logger.getLogger("log").logln(USR.ERROR, leadin()+ "Received unknown control packet type "+
          (char)controlChar);

        return false;

    }

    /** Respond to a ping with an echo */
    synchronized boolean pingResponse(Datagram dg)
    {
       
        Address dst= dg.getSrcAddress();
        int port= dg.getSrcPort();
        int dstPort= dg.getSrcPort();
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Responding to ping with echo to "+dst+
         ":"+dstPort);
        int id= dst.asInteger();
        return echo(id,port);
    }

    /** Routing table received via netIF */
    synchronized void receiveRoutingTable(String tab, NetIF netIF)
    {   
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Received routing table from " + netIF);

        SimpleRoutingTable t;
        try {
            t= new SimpleRoutingTable(tab,netIF);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Received unreadable routing table");
            Logger.getLogger("log").logln(USR.ERROR, leadin()+tab);
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return;
        }
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+ " merging routing table received on "+netIF);
        boolean merged = table_.mergeTables(t,netIF);

        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+ " merged routing table received on "+netIF);
        if (merged) {
            //Logger.getLogger("log").logln(USR.STDOUT, "Send to other interfaces");
            sendToOtherInterfaces(netIF);
        }
    }



    /**
     * A NetIF is closing.
     */
    public synchronized boolean netIFClosing(NetIF netIF) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Remote close from " + netIF);

        if (localNetIF != null) 
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "localNetIF  stats = " + localNetIF.getStats());

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
    
    /** Find the netIF which connects to a given end host 
     * or a connection name
      @return null if none exists*/
    
    public synchronized NetIF findNetIF(String name) {
        int limit = ports.size();
        for (int p = 0;  p < limit; p++) {
            RouterPort port = ports.get(p);

            if (port.equals(RouterPort.EMPTY)) {
                continue;
            } else if (port.getNetIF().getRemoteRouterName().equals(name)) {
                return port.getNetIF();
            } else if (port.getNetIF().getName().equals(name)) {
                return port.getNetIF();
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
        return echo(id, 0);
    }
    
    /** Echo command received */
    public synchronized boolean echo (int id, int port) {
    
        GIDAddress dst= new GIDAddress(id);
        int dstPort= port;
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put("E".getBytes());
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);

        datagram.setDstAddress(dst);
        datagram.setDstPort(dstPort);
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

    /**
     * Tuple class for the queue.
     * It holds a Datagram and the NetIF it came from.
     */
    class DatagramHandle {
        public final Datagram datagram;
        public final NetIF netIF;

        DatagramHandle(Datagram dg, NetIF n) {
            datagram = dg;
            netIF = n;
        }
    }

}
