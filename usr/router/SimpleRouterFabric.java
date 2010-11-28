package usr.router;

import java.util.List;
import usr.logging.*;
import java.util.ArrayList;
import usr.net.*;
import java.net.*;
import usr.protocol.Protocol;
import java.nio.ByteBuffer;
import java.lang.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A RouterFabric within UserSpaceRouting.
 */
public class SimpleRouterFabric implements RouterFabric, NetIFListener, 
    DatagramDevice{
    // The Router this is fabric for
    Router router;
    RouterOptions options_;
    
    Address address_= null;
  
    boolean theEnd=false;

    // A List of RouterPorts
    ArrayList<RouterPort> ports;
   
    LinkedBlockingQueue<DatagramHandle> datagramQueue_;
    int biggestQueueSeen = 0;

    boolean drainingQueue = false;
      
    // The localNetIF
    NetIF localNetIF = null;

    FabricDevice fabricDevice_= null;
    // are we running
    boolean running = false;

    // The RoutingTableTransmitter
    RoutingTableTransmitter routingTableTransmitter;

    // the count of the no of Datagrams
    int datagramInCount = 0;
    int datagramOutCount = 0;

    // The RoutingTable
    SimpleRoutingTable table_= null;

    // routing table info
    HashMap<Integer, Integer> routableAddresses_= null;

    NetIF nextUpdateIF_= null;

    HashMap <NetIF, Long> lastTableUpdateTime_;
    HashMap <NetIF, Long> nextTableUpdateTime_;
    
    String name_= "";

    /**
     * Construct a SimpleRouterFabric.
     */
    public SimpleRouterFabric(Router r, RouterOptions opt) {
        router = r;
        options_= opt;
        table_= new SimpleRoutingTable();
        int limit = 32;
        ports = new ArrayList<RouterPort>(limit);
        for (int p=0; p < limit; p++) {
            setupPort(p);
        }

        address_= r.getAddress();

        localNetIF = null;
        name_= router.getName();
        routableAddresses_= new HashMap<Integer,Integer>();

        lastTableUpdateTime_= new HashMap <NetIF, Long>();
        nextTableUpdateTime_= new HashMap <NetIF, Long>();
        datagramQueue_= new LinkedBlockingQueue<DatagramHandle>();
    }

    /**
     * Start me up.
     */
    public boolean start() {
        table_.setListener(this);
        
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");
        // fabric device -- no queueing
        fabricDevice_= new FabricDevice(this, this);
        // Need an out queue to prevent "in->triggers out" style lockups
        //fabricDevice_.setOutQueueDiscipline(FabricDevice.QUEUE_DROPPING);
        //fabricDevice_.setOutQueueLength(0);

        fabricDevice_.setName("RouterControl");
        fabricDevice_.start();
        running = true;

        // start RoutingTableTransmitter
        routingTableTransmitter = new RoutingTableTransmitter(this);
        routingTableTransmitter.start();

        return true;
    }


    /**
     * Stop the RouterFabric.
     */
    public boolean stop() {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + " router fabric stop");
        // stop RoutingTableTransmitter thread
        routingTableTransmitter.terminate();

        //Logger.getLogger("log").logln(USR.ERROR, "Closing ports");
        // close fabric ports
        closePorts();
        fabricDevice_.stop();
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "datagramInCount = " + datagramInCount + " datagramOutCount = " + datagramOutCount + " queue size = "+ datagramQueue_.size()+ " biggest queue size = " + biggestQueueSeen);

        return true;
    }

    /** Send routing table to all other interfaces apart from inter*/
    synchronized void sendToOtherInterfaces(NetIF inter) 
      
    {
        List <NetIF> l= listNetIF();

        if (l == null)  {
            return;
        }
        for (NetIF i: l) {
            if (i.isLocal() == false && !i.equals(inter)) {
                queueRoutingRequest(i);
            }
        }
    }

    /**
     * Get the local NetIF that has the sockets.
     */
    public NetIF getLocalNetIF() {
        return localNetIF;
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
    public void closePorts() {
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
    public void closePort(RouterPort port) {
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
     * Close port.
     */
    public void remoteClosePort(RouterPort port) {
        if (port.equals(RouterPort.EMPTY)) {
            // nothing to do
        } else {
            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "closing port " + port);
            
            NetIF netIF = port.getNetIF();

            if (!netIF.isClosed()) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Closing " + netIF + " stats = " + netIF.getStats());
                
                netIF.remoteClose();
               
                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "closed port " + port);
            } else {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "ALREADY closed port " + port);
            }
            //Logger.getLogger("log").logln(USR.ERROR, "DONE");
        }
    }

    /** Is this datagram for us */
    public boolean ourAddress(Address addr)
    {
        //Logger.getLogger("log").logln(USR.STDOUT, "DATAGRAM WITH NULL ADDRESS");
        if (addr == null )
            return true; 
        if (address_ == null) 
            return false;
        if (addr.asInteger() == address_.asInteger())
            return true;
            
        return (routableAddresses_.get(addr.asInteger()) != null);
    }
    

 
     



    /**
     * Return either the NetIF for the datagram or failing this null to indicate
     unroutable or datagram is for router
     */
    public DatagramDevice getRoute(Datagram dg) {
        Address addr= dg.getDstAddress();
       /* if (dg.getProtocol() == Protocol.CONTROL) {
            System.err.println("Got CONTROL "+ dg.getDstAddress()+" "+
              dg.getDstPort() + " vs "+address_);
        }*/
        if (ourAddress(addr)) {
            if (dg.getDstPort() == 0) {
                System.err.println("For SRF");
                return this;
            } else {
                return localNetIF;
            }
        }
        NetIF netif= table_.getInterface(addr);
        if (netif != null) {
            return netif;
        }
        //System.err.println("null");
        return null;
    }
    
    
    /** Get the Fabric Device which this packet should be sent to */
    public FabricDevice getRouteFabric(Datagram dg) {

        if (ourAddress(dg.getDstAddress())) {
            if (dg.getDstPort() == 0 || dg.getProtocol() == Protocol.CONTROL) {
                 return fabricDevice_;
            }
            else {
                if (localNetIF == null) {// possible only during shutdown
                    return null;
                }
                return localNetIF.getFabricDevice();
            }
        }

        DatagramDevice inter= getRoute(dg);
        if (inter == null) {
            return null;
            
        }
        return inter.getFabricDevice();
    }
    
    
    
    /** Return a TTL expired datagram unless this is a TTL expired datagram */
    synchronized public void TTLDrop(Datagram dg)
    {
        // Can't return datagram with no source
        if (ourAddress(dg.getSrcAddress()))  
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
    
        /** NetIF wants to send a routing Request */
    synchronized void queueRoutingRequest(NetIF netIF) {
        long now= System.currentTimeMillis();
        Long last= lastTableUpdateTime_.get(netIF);
        Long curr= nextTableUpdateTime_.get(netIF);
        if (last == null || curr == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+netIF+" not in nextTableTime");
            return;
        }
        Long next= last + options_.getMinNetIFUpdateTime();
        
        if (next >= curr) // Already updating at this time or earlier 
            return;
        nextTableUpdateTime_.put(netIF,next);
        if (next <= now) {
            routingTableTransmitter.informNewData();
        }
        
    }  
    
        /** Datagram which has arrived is ours */
    public boolean outQueueHandler(Datagram datagram, DatagramDevice device) {
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " receiveOurDatagram ");
        if (running == false) {  // If we're not running simply pretend to have received it
            return true;
        }
    
        if (datagram.getProtocol() == Protocol.CONTROL) {
            processControlDatagram(datagram, device);
        } else {
            processOrdinaryDatagram(datagram, device);
        }
        return true;
    }


    
    /**
     * Process a datagram for ourselves.
     * NetIF is the original NetIF that the datagram was received on.
     */
    synchronized  void  processOrdinaryDatagram(Datagram datagram, DatagramDevice device) {
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + datagramInCount + " GOT ORDINARY DATAGRAM from " + netIF.getRemoteRouterAddress() + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

        Logger.getLogger("log").logln(USR.ERROR, leadin() + datagramInCount + " FABRIC GOT ORDINARY DATAGRAM from "  + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());
        
        return;
    }

    /**
     * Process a control datagram
     * NetIF is the original NetIF that the datagram was received on.
     */
    synchronized boolean  processControlDatagram(Datagram dg,DatagramDevice device) {
       
        NetIF netif= null;
        if (device != null && device.getClass().equals(TCPNetIF.class)) {
            netif= (TCPNetIF)device;
        }
        // forward datagram if there is a local NetIF and port is not zero
        if (localNetIF != null && dg.getDstPort() != 0) {
            //localNetIF.forwardDatagram(dg);
            System.err.println("TODO FORWARD DATAGRAM to ASM");
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

        //Logger.getLogger("log").logln(USR.EXTRA, leadin()+"TCPNetIF: " + datagramInCount + " <- Control Datagram type "+ (char)controlChar + " data "+ dg);

        //Logger.getLogger("log").logln(USR.ERROR, "RECEIVED DATAGRAM CONTROL TYPE "+(char)controlChar);

       

        if (controlChar == 'C') {
            if (netif != null) {
                remoteRemoveNetIF(netif);
            } else {
                String className= "null";
                String name= "null";
                if (device != null) {
                    name= device.getName();
                    className= device.getClass().getName();
                }
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                  "Remote close on object which is not NetIF" + " " +className + " " +
                   name);
                
            }
            return true;
        }
        if (controlChar == 'T') {
            if ((payload.length -1) % 8 != 0) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"Routing table length not multiple of 8");
            }
            if (netif == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                    "Received routing table from object not NetIF");
                if (device != null) {
                  Logger.getLogger("log").logln(USR.ERROR, leadin()+
                    "Sending device "+device.getName()+" "+device.getClass());
                  
                }
                
                SimpleRoutingTable t= null;
                try {
                   t= new SimpleRoutingTable(payload,netif);
                } catch (Exception e) {
                }
                Logger.getLogger("log").logln(USR.ERROR, leadin()+t);
            } else {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+
                //    "Received routing table from "+netif);
                receiveRoutingTable(payload,netif);
            }
            return true;
        }
        
        if (controlChar == 'X') {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Received TTL expired from "+dg.getSrcAddress()
                +":"+dg.getSrcPort());
            return true;
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
    synchronized void receiveRoutingTable(byte []bytes, NetIF netIF)
    {   
        //sLogger.getLogger("log").logln(USR.STDOUT, leadin()+"Received routing table from " + netIF);

        SimpleRoutingTable t;
        try {
            t= new SimpleRoutingTable(bytes,netIF);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Received unreadable routing table");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return;
        }
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+ " merging routing table received on "+netIF);
        boolean merged = table_.mergeTables(t,netIF, options_);

        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+ " merged routing table received on "+netIF);
        if (merged) {
            //Logger.getLogger("log").logln(USR.STDOUT, "Send to other interfaces");
            sendToOtherInterfaces(netIF);
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
    */
    public void setNetIFListener(NetIFListener l) {
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Call to setNETIFListener illegal");
    }
    
    public NetIFListener getNetIFListener() {
        return this;
    }
    
     /** 
     * Get a list of all connected Network Interfaces
     */
    public List<NetIF> listNetIF() {
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
    
    public Address getAddress() {
        return address_;
    } 
    
    public void setAddress(Address a) {
        address_= a;
    }
    
    
    public String getName() {
        return name_;
    }
    
    public void setName(String n) {
        name_= n;
    }
    
    public FabricDevice getFabricDevice() {
        return fabricDevice_;
    }
    
    public boolean sendDatagram(Datagram dg)
    {
        dg.setSrcAddress(router.getAddress());
        return enqueueDatagram(dg);
    }
    
    public boolean enqueueDatagram(Datagram dg)
    {
        try {
           return fabricDevice_.addToInQueue(dg, this);
        } catch (NoRouteToHostException e) {
            
        }
        return false;
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
     * Add a Network Interface to this Router.
     */
    public synchronized RouterPort addNetIF(NetIF netIF) {
        Address address = netIF.getAddress();
        Address remoteAddress = netIF.getRemoteRouterAddress();
        // add this address into the routableAddresses set
        addRoutableAddress(address);
        // is this actually the local NetIF
        boolean localPort= netIF.isLocal();
        // bind NetIF into a port
        RouterPort rp= null;
        if (!localPort) {
            if (address.equals(remoteAddress)) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                  "netIF has same remote and local address");
                return null;
            }
            int nextFree = findNextFreePort();
            rp = new RouterPort(nextFree, netIF);
            ports.set(nextFree, rp);
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "plugged NetIF: " + netIF + " into port " + nextFree);
        }        
        // it is the local port
        if (localPort) {
            if (localNetIF != null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Attempt to create second local multiplex port");
            }
            localNetIF= netIF;
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "added localNetIF: ");
            return null;
        }

        // sort out when to send routing tables to this NetIF
        Long next= System.currentTimeMillis();
        lastTableUpdateTime_.put(netIF,new Long(0));
        nextTableUpdateTime_.put(netIF,next);
        //Logger.getLogger("log").logln(USR.ERROR, "REQUEST NEW ROUTING TABLE UPDATE NOW");
        // send a Routing table immediately
        queueRoutingRequest(netIF);
        
        // add this to the RoutingTable
        if (table_.addNetIF(netIF, options_)) {
            sendToOtherInterfaces(netIF);
            
        }
        return rp;
    }
    
    /**
     * Remove a Network Interface from this Router.
     */
    public synchronized boolean removeNetIF(NetIF netIF) {   
        return doRemove(netIF, false);
    }    
    
     /**
     * Remove a Network Interface from this Router after remote request
     */
    public synchronized boolean remoteRemoveNetIF(NetIF netIF) {
        return doRemove(netIF, true);
    }
        
    /** Do work for remote or normal remove -- onyl difference is in
    sending control close */    
    public boolean doRemove(NetIF netIF, boolean remote) {
        Address address = netIF.getAddress();
        // remove this address from the routableAddresses set
        removeRoutableAddress(address);

        // it is the local port
        if (netIF.isLocal()) {
            closeLocalNetIF();
            return true;
        }

        // check Ports
        RouterPort port = findNetIF(netIF);

        if (port != null) {
            // disconnect netIF from port
             // Remove table update times
            lastTableUpdateTime_.remove(netIF);
            nextTableUpdateTime_.remove(netIF);
            if (remote) {
                remoteClosePort(port);
            } else {
                closePort(port);
            }
            resetPort(port.getPortNo());
            if (table_.removeNetIF(netIF)) {
                sendToOtherInterfaces(netIF);
            }
            routingTableTransmitter.informNewData();
            return true;
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+netIF+" NOT CONNECTED TO PORT");
            // didn't find netIF in any RouterPort
            return false;
        }
    }
    
   

    
    /** Track routable addresses for this router */
    void addRoutableAddress(Address a) {
        Integer aCount= routableAddresses_.get(a.asInteger());
        if (aCount == null) {
            routableAddresses_.put(a.asInteger(),1);
        } else {
            routableAddresses_.put(a.asInteger(),(aCount+1));
        }
    }
    
    void removeRoutableAddress(Address a) {
        Integer aCount= routableAddresses_.get(a.asInteger());
        if (aCount == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() +
               "Request to remove address "+a+" not on routable list");
            return;
        }
        if (aCount == 1) {
            routableAddresses_.remove(a.asInteger());
        } else {
            routableAddresses_.put(a.asInteger(),aCount-1);
        }
    } 

    public void closedDevice(DatagramDevice dd) {
        if (dd instanceof NetIF) {
            remoteRemoveNetIF((NetIF)dd);
            return;
        }
        Logger.getLogger("log").logln(USR.ERROR, leadin()+dd+" Datagram device reports as broken");
    }
    


   
    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RF = "SRF: ";
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

  
    /**
     * A Thread that sends out the Routing Table
     */
class RoutingTableTransmitter extends Thread {
        // The Fabric
        SimpleRouterFabric fabric;

        // is running
        boolean running = false;


        Object waitObj_= null;
        /**
         * Constructor
         */
        public RoutingTableTransmitter(SimpleRouterFabric srf) {
            fabric = srf;
            waitObj_= new Object();

            setName("RoutingTableTransmitter-" + fabric.hashCode());
        }


        /**
         * The main thread loop.
         * It occasionally checks to see if it needs to 
         * send a routing table.
         */
        public void run() {
            running = true;

            long nextUpdateTime;

            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "RoutingTableTransmitter Running");

            while (running) {

                long now=  System.currentTimeMillis();

                // dont need to do this every time, but how
                nextUpdateTime = calcNextTableSendTime();

                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "run TIME: "+now + " nextUpdateTime: " + nextUpdateTime + " diff: " + (nextUpdateTime - now));
            
                if (nextUpdateTime <= now) {
                    //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Sending table");

                    sendNextTable();
                    continue;
                }
            
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Waiting Until: "+ nextUpdateTime);
                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "run Waiting For: "+ ((float)(nextUpdateTime-now))/1000);
                //Logger.getLogger("log").logln(USR.ERROR, "Time now "+ now);

                if (running) {
                    waitUntil(nextUpdateTime);
                }
                
                //Logger.getLogger("log").logln(USR.ERROR, "Running is "+running);
            }


            //theEnd();
        }


      
          /** Calculate when the next table send event is */
    synchronized long calcNextTableSendTime() {
        long now= System.currentTimeMillis();
        long nextUpdateTime= now+options_.getMaxCheckTime();
        nextUpdateIF_= null;
        for (NetIF n: listNetIF()) {
            if (n.isLocal())
                continue;
            Long next= nextTableUpdateTime_.get(n);
            if (next == null) {
                //for whatever reason this is not in table -- add it
                nextTableUpdateTime_.put(n,nextUpdateTime);
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
    

        /**
         * Notify 
         */
        public void informNewData() {
            synchronized(waitObj_) {
                waitObj_.notify();
            }

        }


    /**
     * Wait until a specified absolute time is milliseconds.
     */
    void waitUntil(long time){
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Wait until " + time);
        long now = System.currentTimeMillis();

        if (time <= now)
            return;
        try {
            long timeout = time - now + 1;

            synchronized (waitObj_) {

                waitObj_.wait(timeout);

            }

        } catch(InterruptedException e){
        }
    }
    
  
    /** Now send a routing table */
    synchronized void sendNextTable() {
        //Logger.getLogger("log").log(USR.EXTRA, "T");
        NetIF inter = nextUpdateIF_;
        long now= System.currentTimeMillis();
        if (inter == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "No table to send");
            
            return;
        }

        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + now+" sending table " + table_ + " for "+ inter);

        byte[]table= table_.toBytes();

        byte []toSend= new byte[table.length+1];
        if (table.length % 8 != 0) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Routing table length not multiple of 8");
        }
        toSend[0]= (byte)'T';
        System.arraycopy(table, 0, toSend,1,table.length);
        ByteBuffer buffer = ByteBuffer.allocate(table.length+1);
        buffer.put(toSend);
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
        datagram.setDstAddress(inter.getRemoteRouterAddress());
        datagramOutCount++;
        sendDatagram(datagram);
        lastTableUpdateTime_.put(inter,now);
        nextTableUpdateTime_.put(inter,now+options_.getMaxNetIFUpdateTime());
        //Logger.getLogger("log").logln(USR.ERROR, "Next table update time"+nextUpdateTime_);

    }
      

      

        /**
         * Stop the RoutingTableTransmitter
         */
        public void terminate() {
                try {
                    running = false;
                    
                    this.interrupt();
                } catch (Exception e) {
                    //Logger.getLogger("log").logln(USR.ERROR, "RoutingTableTransmitter: Exception in terminate() " + e);
                }
        }

        String leadin() {
            final String RF = "SRF.RTT ";

            RouterController controller = fabric.router.getRouterController();

            return controller.getName() + " " + RF;
       }

    }

}
