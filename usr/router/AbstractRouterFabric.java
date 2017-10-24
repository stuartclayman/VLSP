package usr.router;

import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import usr.common.TimedThread;
import usr.logging.Logger;
import usr.logging.USR;
import usr.common.ANSI;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.protocol.Protocol;

/**
 * The main abstract code for a RouterFabric within UserSpaceRouting.
 */
public abstract class AbstractRouterFabric implements RouterFabric, NetIFListener, DatagramDevice {
    // The Router this is fabric for
    Router router;
    RouterOptions options_;

    Address address_ = null;

    boolean theEnd = false;

    // A List of RouterPorts
    ArrayList<RouterPort> ports;

    int biggestQueueSeen = 0;

    // The localNetIF
    NetIF localNetIF = null;

    FabricDevice fabricDevice_ = null;
    // are we running
    boolean running = false;

    // The RoutingTableTransmitter
    RoutingTableTransmitter routingTableTransmitter;

    // The RoutingTable
    RoutingTable table_ = null;

    // routing table info
    HashMap<Address, Integer> routableAddresses_ = null;

    NetIF nextUpdateIF_ = null;

    HashMap<NetIF, Long> lastTableUpdateTime_;
    HashMap<NetIF, Long> nextTableUpdateTime_;

    String name_ = "";

    FabricState state = FabricState.PRE_INIT;

    Semaphore semaphore;


    /**
     * Construct a AbstractRouterFabric.
     */
    protected AbstractRouterFabric(Router r, RouterOptions opt) {
        router = r;
        options_ = opt;
    }

    /**
     * Initialisation
     */
    @Override
    public boolean init() {
        table_ = newRoutingTable();
        int limit = 32;
        ports = new ArrayList<RouterPort>(limit);

        for (int p = 0; p < limit; p++) {
            setupPort(p);
        }

        address_ = router.getAddress();

        localNetIF = null;
        name_ = router.getName();
        routableAddresses_ = new HashMap<Address, Integer>();

        lastTableUpdateTime_ = new HashMap<NetIF, Long>();
        nextTableUpdateTime_ = new HashMap<NetIF, Long>();

        semaphore = new Semaphore(1);

        state = FabricState.POST_INIT;

        return true;
    }

    /**
     * Start me up.
     */
    @Override
    public boolean start() {
        table_.setListener(this);

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "start");
        // fabric device -- no queueing
        fabricDevice_ = new FabricDevice(this, this);
        // Need an out queue to prevent "in->triggers out" style lockups
        //fabricDevice_.setOutQueueDiscipline(FabricDevice.QUEUE_DROPPING);
        //fabricDevice_.setOutQueueLength(0);

        fabricDevice_.setName("RouterControl");
        fabricDevice_.start();
        running = true;

        // start RoutingTableTransmitter
        routingTableTransmitter = new RoutingTableTransmitter(this);
        routingTableTransmitter.start();

        state = FabricState.STARTED;

        return true;
    }

    /**
     * Stop the RouterFabric.
     */
    @Override
    public boolean stop() {

        state = FabricState.STOPPING;

        // stop RoutingTableTransmitter thread
        routingTableTransmitter.terminate();
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " routingTableTransmitter terminated");

        //Logger.getLogger("log").logln(USR.ERROR, "Closing ports");
        // close fabric ports
        closePorts();
        fabricDevice_.stop();

        state = FabricState.STOPPED;

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Router Fabric stopped");
        
        return true;
    }


    /**
     * Get the state 
     */
    public FabricState getState() {
        return state;
    }



    /** Send routing table to all other interfaces apart from inter*/
    void sendToOtherInterfaces(NetIF inter) {
        synchronized (ports) {
            List<NetIF> l = listNetIF();

            if (l == null) {
                return;
            }

            for (NetIF i : l) {
                if (i == null) {
                    continue;
                }

                if (i.isLocal() == false && !i.equals(inter)) {
                    queueRoutingRequest(i);
                }
            }
        }
    }

    /**
     * Get the Router this Fabric is part of
     */
    @Override
    public Router getRouter() {
        return router;
    }

    /**
     * Get the local NetIF that has the sockets.
     */
    @Override
    public NetIF getLocalNetIF() {
        return localNetIF;
    }

    /**
     * Get port N.
     */
    @Override
    public RouterPort getPort(int p) {
        return ports.get(p);
    }

    /**
     * Get a list of all the ports with Network Interfaces.
     */
    @Override
    public List<RouterPort> listPorts() {
        return ports;
    }

    /**
     * Close ports.
     */
    @Override
    public void closePorts() {
        //Logger.getLogger("log").logln(USR.ERROR, "Local NetIF close");
        if (localNetIF != null) {
            closeLocalNetIF();
        }

        //Logger.getLogger("log").logln(USR.ERROR, "Closing ports");
        for (int i = 0; i < ports.size(); i++) {
            // Logger.getLogger("log").logln(USR.ERROR, "Closing port "+i);
            RouterPort port = ports.get(i);

            if (port == null) {
                continue;
            }
            closePort(port);
            int pno = port.getPortNo();

            if (pno >= 0) {
                resetPort(port.getPortNo());
            }
        }

    }

    void closeLocalNetIF() {
        synchronized (ports) {
            if (localNetIF != null) {
                //Logger.getLogger("log").logln(USR.ERROR, "TRYING TO CLOSE LOCALNETIF");
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Closing " + localNetIF + " stats = " + localNetIF.getStats());

                localNetIF.close();
                //Logger.getLogger("log").logln(USR.ERROR, "IT'S SHUT");
            }

            localNetIF = null;
        }
    }

    /**
     * Close port.
     */
    @Override
    public void closePort(RouterPort port) {
        if (port.equals(RouterPort.EMPTY)) {
            // nothing to do
        } else {
            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "closing port " + port);

            NetIF netIF = port.getNetIF();

            if (!netIF.isClosed()) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Closing NetIF " + netIF + " stats = " + netIF.getStats());

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
    @Override
    public boolean ourAddress(Address addr) {
        //Logger.getLogger("log").logln(USR.STDOUT, "DATAGRAM WITH NULL ADDRESS");
        if (addr == null ) {
            return true;
        }

        if (address_ == null) {
            return false;
        }

        if (addr.equals(address_)) {
            return true;
        }


        Object obj = routableAddresses_.get(addr);

        return (obj != null);
    }

    /**
     * Return either the NetIF for the datagram or failing this null to indicate
     * unroutable or datagram is for router
     */
    public DatagramDevice getRoute(Datagram dg) throws NoRouteToHostException {
        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "getRoute() for " + dg);


        Address addr = dg.getDstAddress();

        /* if (dg.getProtocol() == Protocol.CONTROL) {
           System.err.println("Got CONTROL "+ dg.getDstAddress()+" "+ dg.getDstPort() + " vs "+address_);
           }*/

        if (ourAddress(addr)) {
            if (dg.getDstPort() == 0) {
                return this;
            } else {
                if (localNetIF == null) {
                    throw new NoRouteToHostException("No route to host " + addr.asTransmitForm());
                }
                return localNetIF;
            }
        }

        NetIF netif = table_.getInterface(addr);

        if (netif != null) {
            return netif;
        }
        //System.err.println("null");
        throw new NoRouteToHostException("No route to host " + addr.asTransmitForm());
    }

    /** Get the Fabric Device which this packet should be sent to */
    @Override
    public FabricDevice lookupRoutingFabricDevice(Datagram dg) throws NoRouteToHostException {

        if (ourAddress(dg.getDstAddress())) {
            if (dg.getDstPort() == 0 || dg.getProtocol() == Protocol.CONTROL) {
                return fabricDevice_;
            } else {
                if (localNetIF == null) { // possible only during shutdown
                    throw new NoRouteToHostException("No route to host " + "localNet");
                }
                return localNetIF.getFabricDevice();
            }
        }

        DatagramDevice inter ;

        try {
            inter = getRoute(dg);

            // shouldn't happen
            if (inter == null) {
                throw new NoRouteToHostException("No route to host " + dg.getDstAddress().asTransmitForm());
            }
        } catch (NoRouteToHostException nrhe) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin() +
            //                              "Cannot find interface for Datagram"+dg);
            //Logger.getLogger("log").logln(USR.ERROR, leadin() +
            //                              "table = " + table_);
            throw nrhe;
        }

        FabricDevice f = inter.getFabricDevice();

        if (f == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                          "Cannot find fabric device for interface"+inter);
            throw new NoRouteToHostException("No route to host " + dg.getDstAddress().asTransmitForm());
        }
        return f;
    }

    /** Return a TTL expired datagram unless this is a TTL expired datagram */
    @Override
    public void TTLDrop(Datagram dg) {
        // Can't return datagram with no source
        if (ourAddress(dg.getSrcAddress())) {
            return;
        }

        // Don't return TTL expired datagram.
        if (dg.getProtocol() == Protocol.CONTROL) {
            byte [] payload = dg.getPayload();

            if (payload.length > 0 && (char)payload[0] == 'X') {
                return;
            }
        }
        // OK -- send TTL expired datagram
        Address dst = dg.getSrcAddress();
        byte[] buffer = new byte[1];
        buffer[0] = 'X';
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);

        datagram.setDstAddress(dst);
        try {
            sendDatagram(datagram);
        } catch (NoRouteToHostException e) {
            // Route lost, never mind
        }
    }

    /** NetIF wants to send a routing Request */
    void queueRoutingRequest(NetIF netIF) {
        long now = System.currentTimeMillis();
        Long last = lastTableUpdateTime_.get(netIF);
        Long curr = nextTableUpdateTime_.get(netIF);

        if (last == null || curr == null) {
            if (last == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+netIF+" not in lastTableTime");
            }

            if (curr == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+netIF+" not in nextTableTime");
            }


            return;
        }
        Long next = last + options_.getMinNetIFUpdateTime();

        if (next >= curr) { // Already updating at this time or earlier
            return;
        }
        nextTableUpdateTime_.put(netIF, next);

        if (next <= now) {
            routingTableTransmitter.informNewData();
        }

    }

    /** Datagram which has arrived is ours */
    @Override
    public synchronized boolean recvDatagramFromDevice(Datagram datagram, DatagramDevice device) {

        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " receiveOurDatagram ");
        if (running == false) {  // If we're not running simply pretend to have received it
            return true;
        }

        if (datagram.getProtocol() == Protocol.CONTROL) {
            processControlDatagram(datagram, device);
        } else if (datagram.getProtocol() == Protocol.DATA) {
            processOrdinaryDatagram(datagram, device);
        } else {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "datagram protocol"+
                                          datagram.getProtocol());
        }
        return true;
    }

    /**
     * Process a datagram for ourselves.
     * NetIF is the original NetIF that the datagram was received on.
     */
    void  processOrdinaryDatagram(Datagram datagram, DatagramDevice device) {

        Logger.getLogger("log").logln(USR.ERROR,
                                      leadin() + " Fabric received ordinary datagram from "  + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" +
                                      datagram.getDstPort());
        byte [] payl = datagram.getPayload();
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Length "+ payl.length + " Contents "+ java.util.Arrays.toString(payl));

        if (payl.length > 0) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "First char is "+(char)payl[0]);
        }
        return;
    }

    /**
     * Process a control datagram
     * NetIF is the original NetIF that the datagram was received on.
     */
    boolean  processControlDatagram(Datagram dg, DatagramDevice device) {
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " processControlDatagram " + dg + " from " + device);

        NetIF netif = null;

        if (device != null && device instanceof NetIF) {
            netif = (NetIF)device;
        }


        // forward datagram if there is a local NetIF and port is not zero
        if (localNetIF != null && dg.getDstPort() != 0) {
            //localNetIF.forwardDatagram(dg);
            Logger.getLogger("log").logln(USR.ERROR,
                                          leadin()+"TODO FORWARD DATAGRAM to ASM");
            return true;
        }
        byte[] payload = dg.getPayload();

        if (payload.length == 0) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"GOT LENGTH ZERO DATAGRAM");
            return true;
        }
        byte controlChar = payload[0];

        if (controlChar == 'C') {
            if (netif != null) {
                remoteRemoveNetIF(netif);
            } else {
                String className = "null";
                String name = "null";

                if (device != null) {
                    name = device.getName();
                    className = device.getClass().getName();
                }
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                              "Remote close on object which is not NetIF" + " " +className + " " +
                                              name);

            }
            return true;
        }

        if (controlChar == 'T') {
            if (netif == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                              "Received routing table from object not NetIF");

                if (device != null) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                                  "Sending device "+device.getName()+" "+device.getClass());

                }

                RoutingTable t = null;
                try {
                    t = decodeRoutingTable(payload, netif);
                } catch (Exception e) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin()+ e);
                    e.printStackTrace();
                }

                Logger.getLogger("log").logln(USR.ERROR, leadin()+t);
            } else {
                receiveRoutingTable(payload, netif);
            }
            return true;
        }

        if (controlChar == 'W') {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          leadin() + "Received withdraw message from "
                                          + dg.getSrcAddress() + ":" + dg.getSrcPort());
            receiveAddressWithdraw(payload, netif);
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
        Logger.getLogger("log").logln(USR.ERROR, leadin()+ "Received unknown control packet type from "+
                                      dg.getDstAddress() + ": " + Integer.toString((int)controlChar));

        return false;

    }

    /** Respond to a ping with an echo */
    boolean pingResponse(Datagram dg) {

        Address dst = dg.getSrcAddress();
        int port = dg.getSrcPort();
        int dstPort = dg.getSrcPort();
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Responding to ping with echo to "+dst+
                                      ":"+dstPort);

        return echo(dst, port);
    }

    /** Request to withdraw address received via netIF*/
    void receiveAddressWithdraw(byte [] bytes, NetIF netIF) {
        ArrayList<Address> addresses;
        addresses = translateWithdraw(bytes);
        boolean changed = false;
        synchronized (table_) {
            for (Address a : addresses) {
                if (table_.removeAddress(a)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            withdrawToOtherInterfaces(addresses, netIF);
        }
    }

    @Override
    public void sendGoodbye() {
        ArrayList<Address> addresses = new ArrayList<Address>();
        Address me = router.getAddress();

        if (me != null) {
            addresses.add(me);
        }

        for (Address a : routableAddresses_.keySet()) {
            if (!a.equals(me)) {
                addresses.add(a);
            }
        }

        for (Address a : addresses) {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          leadin() + "Sending route withdraw message for " + a);
        }

        //// sclayman 20131202 - turn off sendind withdraw msgs
        //// withdrawToOtherInterfaces(addresses, null);
    }

    /** Send a withdrawal message for a given address
     * to all interfaces on this network
     * interface*/
    public void withdrawToOtherInterfaces(ArrayList<Address> addr, NetIF netIF) {
        synchronized (ports) {
            for (RouterPort p : ports) {
                NetIF nf = p.getNetIF();

                if ((nf == null) || (nf == netIF) || (nf == localNetIF)) {
                    continue;
                }

                byte [] message = constructWithdrawMessage(addr);
                Datagram dg = DatagramFactory.newDatagram(Protocol.CONTROL,
                                                          message);
                dg.setSrcAddress(router.getAddress());
                Address a = nf.getRemoteRouterAddress();

                if (a == null) {
                    Logger.getLogger("log").logln(USR.ERROR,
                                                  leadin() + "Network interface " + nf +
                                                  " has no address");
                    continue;
                }

                dg.setDstAddress(a);
                try {
                    sendDatagram(dg);
                } catch (NoRouteToHostException e) {
                    Logger.getLogger("log").logln(USR.STDOUT,
                                                  leadin() + "Cannot route CONTROL withdraw message to "
                                                  + nf.getRemoteRouterAddress());
                }
            }
        }
    }

    /** Convert a series of addresses to a withdraw message in bytes */
    private byte [] constructWithdrawMessage(ArrayList<Address> addr) {
        int totSize = 0;

        for (Address a : addr) {
            totSize += a.size();
        }

        byte [] message = new byte[1 + totSize];
        message[0] = 'W';
        int pos = 1;

        for (Address a : addr) {
            System.arraycopy(a.asByteArray(), 0, message, pos, a.size());
            pos += a.size();
        }

        return message;
    }

    /** Translate message to address array */
    private ArrayList<Address> translateWithdraw(byte [] bytes) {
        ByteBuffer wrapper = ByteBuffer.wrap(bytes);

        ArrayList<Address> addresses = new ArrayList<Address>();
        Address a;
        try {
            a = AddressFactory.newAddress(0);
        } catch (UnknownHostException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()
                                          + "Cannot create new address in translateWithdraw");
            return addresses;
        }

        int pos = 1;

        while (pos < bytes.length) {
            wrapper.position(pos);
            byte[] addrbytes = new byte[a.size()];
            wrapper.get(addrbytes);
            Address a2 = AddressFactory.newAddress(addrbytes);
            addresses.add(a2);
            pos += a.size();
        }

        return addresses;
    }


    /** Routing table received via netIF */
    void receiveRoutingTable(byte [] bytes, NetIF netIF) {
        //Logger.getLogger("log").logln(USR.STDOUT, leadin()+ System.currentTimeMillis() + " receiveRoutingTable: Received routing table from " + netIF);

        RoutingTable t;
        try {
            t = decodeRoutingTable(bytes, netIF);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Received unreadable routing table");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            return;
        }

        if (t == null) {
            Logger.getLogger("log").logln(1<<6, leadin()+ System.currentTimeMillis() + " not merging null routing table received on "+netIF);

            return;
        } else {
            Logger.getLogger("log").logln(1<<6, leadin()+ System.currentTimeMillis() + " merging routing table received on "+netIF);



            // 25122014 sclayman
            long now = System.currentTimeMillis();

            Logger.getLogger("log").logln(1<<6, leadin() + now+ " recv table to interface "+ netIF);
            Logger.getLogger("log").logln(1<<6, leadin() + "\nsize " + bytes.length + " = 5+" +
                                          (bytes.length-5) + " -> " + t.showTransmitted());




            boolean merged = false;
            synchronized (table_) {
                merged = table_.mergeTables(t, netIF, options_);

                Logger.getLogger("log").logln(1<<6, leadin()+ System.currentTimeMillis() + " merged routing table received on "+netIF + "\nResult = " + table_);

            }

            if (merged) {
                //Logger.getLogger("log").logln(USR.STDOUT, ANSI.GREEN + "Send to other interfaces" + ANSI.RESET_COLOUR);
                sendToOtherInterfaces(netIF);
            }
        }
    }

    /*
     * Port processing
     */

    /**
     * Setup a port
     */
    void setupPort(int p) {
        synchronized (ports) {
            ports.add(p, RouterPort.EMPTY);
        }
    }

    /**
     * Reset a port
     */
    void resetPort(int p) {
        synchronized (ports) {
            ports.set(p, RouterPort.EMPTY);
        }
    }

    /**
     * Return the routing table
     */
    @Override
    public RoutingTable getRoutingTable() {
        return table_;
    }

    /**
     * Find the port a NetIF is in.
     * Skip through all ports to find a NetIF
     * @return null if a NetIF is not found.
     */
    RouterPort findNetIF(NetIF netIF) {
        synchronized (ports) {
            int limit = ports.size();

            for (int p = 0; p < limit; p++) {
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
    }

    /** Find the netIF which connects to a given end host
     * or a connection name
     * @return null if none exists*/

    @Override
    public NetIF findNetIF(String name) {
        synchronized (ports) {
            int limit = ports.size();

            for (int p = 0; p < limit; p++) {
                RouterPort port = ports.get(p);

                if (port.equals(RouterPort.EMPTY)) {
                    continue;

                } else {
                    /*
                      Logger.getLogger("log").logln(USR.ERROR, leadin() + "findNetIF " + name +
                      " getRemoteRouterAddress = " + port.getNetIF().getRemoteRouterAddress() +
                      " getRemoteRouterName = " + port.getNetIF().getRemoteRouterName() +
                      " getName = " + port.getNetIF().getName() +
                      "\n");
                    */

                    Address addr = null;
                    try {
                        addr = AddressFactory.newAddress(name);
                    } catch (java.net.UnknownHostException e) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot create address from "+name);
                    }

                    if (port.getNetIF().getRemoteRouterAddress().asTransmitForm().equals(name)) {
                        // try by string form
                        return port.getNetIF();
                    } else if (port.getNetIF().getRemoteRouterAddress().equals(addr)) {
                        // try by addr
                        return port.getNetIF();
                    } else if (port.getNetIF().getRemoteRouterName().equals(name)) {
                        // try by router name
                        return port.getNetIF();
                    } else if (port.getNetIF().getName().equals(name)) {
                        // try by NetIF name
                        return port.getNetIF();
                    } else {
                        ;
                    }
                }
            }

            return null;
        }
    }


    /**
     * Set the netIF weight associated with a link to a certain router name
     */
    @Override
    public boolean setNetIFWeight(String name, int weight) {
        NetIF netIF = findNetIF(name);

        if (netIF == null) {
            return false;
        } else {
            // update routing table
            table_.setNetIFWeight(netIF, weight);

            // must be done after updating table
            // set weight on NetIF
            netIF.setWeight(weight);

            return true;
        }
    }

    /**
     */
    @Override
    public void setNetIFListener(NetIFListener l) {
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Call to setNETIFListener illegal");
    }

    @Override
    public NetIFListener getNetIFListener() {
        return this;
    }

    /**
     * Get a list of all connected Network Interfaces
     */
    @Override
    public List<NetIF> listNetIF() {
        ArrayList<NetIF> list = new ArrayList<NetIF>();
        int limit = ports.size();

        for (int p = 0; p < limit; p++) {
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
    int findNextFreePort() {
        synchronized (ports) {
            int limit = ports.size();

            for (int p = 0; p < limit; p++) {

                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Port  " + p + " = " + ports.get(p));

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
    }

    /** Ping command received */
    @Override
    public boolean ping (Address dst) {
        //Address dst = AddressFactory.newAddress(id);
        byte[] buffer = new byte[1];
        buffer[0] = 'P';
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
        datagram.setDstAddress(dst);
        try {
            sendDatagram(datagram);
        } catch (NoRouteToHostException e) {
            return false;
        }

        return true;
    }

    @Override
    public Address getAddress() {
        return address_;
    }

    @Override
    public void setAddress(Address a) {
        address_ = a;
    }

    @Override
    public String getName() {
        return name_;
    }

    @Override
    public void setName(String n) {
        name_ = n;
    }

    @Override
    public FabricDevice getFabricDevice() {
        return fabricDevice_;
    }

    @Override
    public boolean sendDatagram(Datagram dg) throws NoRouteToHostException {
        // SC 20130620
        // dg.setSrcAddress(router.getAddress());
        return enqueueDatagram(dg);
    }

    @Override
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException {
        try {
            return fabricDevice_.addToInQueue(dg, this);
        } catch (NoRouteToHostException e) {
            throw e;
        }
    }

    /** Echo command received */
    @Override
    public boolean echo (Address addr) {
        return echo(addr, 0);
    }

    /** Echo command received */
    public boolean echo (Address dst, int port) {
        //Address dst = AddressFactory.newAddress(id);
        int dstPort = port;
        byte [] buffer = new byte[1];
        buffer[0] = 'E';
        Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);

        datagram.setDstAddress(dst);
        datagram.setDstPort(dstPort);
        try {
            sendDatagram(datagram);
        } catch (NoRouteToHostException e) {
            return false;
        }

        return true;
    }

    /**
     * Add a Network Interface to this Router.
     */
    @Override
    public RouterPort addNetIF(NetIF netIF) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "addNetIF NetIF: " + netIF.getName() + " " + netIF.getAddress() );

        try {
            synchronized (ports) {
                Address address = netIF.getAddress();
                Address remoteAddress = netIF.getRemoteRouterAddress();
                // add this address into the routableAddresses set
                addRoutableAddress(address);
                // is this actually the local NetIF
                boolean localPort = netIF.isLocal();
                // bind NetIF into a port
                RouterPort rp = null;

                // it is the local port
                if (localPort) {
                    if (localNetIF != null) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Attempt to create second local multiplex port");
                    }
                    localNetIF = netIF;
                    Logger.getLogger("log").logln(USR.STDOUT,
                                                  leadin() + "added localNetIF: " + localNetIF.getName() + " " +
                                                  localNetIF.getAddress());
                    return null;
                } else {
                    addRoutableAddress(address);
                }

                if (!localPort) {
                    if (address.equals(remoteAddress)) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                                      "netIF has same remote and local address");
                        return null;
                    }
                    int nextFree = findNextFreePort();
                    rp = new RouterPort(nextFree, netIF);
                    ports.set(nextFree, rp);
                    netIF.setRouterPort(rp);

                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "plugged NetIF: " + netIF + " into port " + nextFree);
                }

                synchronized (table_) {

                    if (table_.addNetIF(netIF, options_)) {
                        sendToOtherInterfaces(netIF);

                    }
                }

                // sort out when to send routing tables to this NetIF
                Long next = System.currentTimeMillis();
                lastTableUpdateTime_.put(netIF, new Long(0));
                nextTableUpdateTime_.put(netIF, next);
                queueRoutingRequest(netIF);


                return rp;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Remove a Network Interface from this Router.
     * synchronized to prevent multiple calls
     */
    @Override
    public boolean removeNetIF(NetIF netIF) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "removeNetIF NetIF: " + netIF.getName() + " " + getAddress() );

        //synchronized (ports) {  // this can lock up on link end
        return doRemove(netIF, false);
        //}
    }

    /**
     * Remove a Network Interface from this Router after remote request
     * synchronized to prevent multiple calls
     */
    public boolean remoteRemoveNetIF(NetIF netIF) {
        //synchronized (ports) { // this can lock up on link end

        if (state == RouterFabric.FabricState.STOPPING || state == RouterFabric.FabricState.STOPPED) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Already stopping when remoteRemoveNetIF() called");
            return false;
        }
            

        return doRemove(netIF, true);
        //}
    }

    /** Do work for remote or normal remove -- onyl difference is in
     * sending control close */
    public boolean doRemove(NetIF netIF, boolean remote) {
        try {

            // have single access through this code
            // was protected by 'synchronized (ports)'
            // but that causes lockups
            semaphore.acquire();

            Address address = netIF.getAddress();
            // remove this address from the routableAddresses set

            // it is the local port
            if (netIF.isLocal()) {
                removeRoutableAddress(address);
                closeLocalNetIF();
                return true;
            }

            // check Ports
            RouterPort port = findNetIF(netIF);

            if (port != null) {
                // disconnect netIF from port

                removeRoutableAddress(address);
                // Remove table update times
                lastTableUpdateTime_.remove(netIF);
                nextTableUpdateTime_.remove(netIF);

                if (remote) {
                    remoteClosePort(port);
                } else {
                    closePort(port);
                }
                netIF.setRouterPort(null);
                resetPort(port.getPortNo());

                Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Unplug " + netIF.getName()+ " from port " + port.getPortNo());


                synchronized (table_) {
                    if (table_.removeNetIF(netIF)) {
                        sendToOtherInterfaces(netIF);
                    }
                }
                routingTableTransmitter.informNewData();
                return true;
            } else {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+netIF+ " second attempt to remove");
                // didn't find netIF in any RouterPort
                return false;
            }

        } catch (InterruptedException ie) {
            return false;

        } finally {
            semaphore.release();
        }

    }

    /** Track routable addresses for this router */
    void addRoutableAddress(Address a) {
        synchronized (routableAddresses_) {
            Integer aCount = routableAddresses_.get(a);

            if (aCount == null) {
                routableAddresses_.put(a, 1);
            } else {
                routableAddresses_.put(a, (aCount+1));
            }
        }
    }

    void removeRoutableAddress(Address a) {
        synchronized (routableAddresses_) {
            Integer aCount = routableAddresses_.get(a);

            if (aCount == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() +
                                              "Request to remove address "+a+" not on routable list");
                return;
            }

            if (aCount == 1) {
                routableAddresses_.remove(a);
            } else {
                routableAddresses_.put(a, aCount-1);
            }
        }
    }

    @Override
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
        final String RF = ": ";
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
    class RoutingTableTransmitter extends TimedThread {
        // The Fabric
        RouterFabric fabric;

        // is running
        boolean running = false;


        Object waitObj_ = null;
        /**
         * Constructor
         */
        public RoutingTableTransmitter(RouterFabric srf) {
            fabric = srf;
            waitObj_ = new Object();

            setName("/" + fabric.getName() + "/RoutingTableTransmitter/" + fabric.hashCode());
        }

        /**
         * The main thread loop.
         * It occasionally checks to see if it needs to
         * send a routing table.
         */
        @Override
        public void run() {
            running = true;

            long nextUpdateTime;

            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "RoutingTableTransmitter Running");

            while (running) {

                long now = System.currentTimeMillis();

                // dont need to do this every time, but how
                nextUpdateTime = calcNextTableSendTime();

                //Logger.getLogger("log").logln(USR.ERROR, leadin() + "run TIME: "+now + " nextUpdateTime: " + nextUpdateTime + "
                // diff: " + (nextUpdateTime - now));

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
        long calcNextTableSendTime() {
            synchronized (nextTableUpdateTime_) {
                long now = System.currentTimeMillis();
                long nextUpdateTime = now+options_.getMaxCheckTime();
                nextUpdateIF_ = null;

                for (NetIF n : listNetIF()) {
                    if (n.isLocal()) {
                        continue;
                    }
                    Long next = nextTableUpdateTime_.get(n);

                    if (next == null) {
                        //for whatever reason this is not in table -- add it
                        nextTableUpdateTime_.put(n, nextUpdateTime);
                        continue;
                    }

                    //Logger.getLogger("log").logln(USR.ERROR, "Considering update from "+n+" at time "+next);
                    if (next < nextUpdateTime) {
                        //Logger.getLogger("log").logln(USR.ERROR, "Next update interface is now "+n);
                        nextUpdateTime = next;
                        nextUpdateIF_ = n;
                    }
                }
                //Logger.getLogger("log").logln(USR.ERROR, "Next event at "+nextUpdateTime+" from "+nextUpdateIF_);
                return nextUpdateTime;
            }
        }

        /**
         * Notify
         */
        public void informNewData() {
            synchronized (waitObj_) {
                waitObj_.notify();
            }

        }

        /**
         * Wait until a specified absolute time is milliseconds.
         */
        void waitUntil(long time) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Wait until " + time);
            long now = System.currentTimeMillis();

            if (time <= now) {
                return;
            }
            try {
                long timeout = time - now + 1;

                synchronized (waitObj_) {
                    //Logger.getLogger("log").logln(USR.ERROR, leadin()+" waitUntil WAIT " + timeout);

                    waitObj_.wait(timeout);

                }

            } catch (InterruptedException e) {
            }
        }

        /** Now send a routing table */
        void sendNextTable() {

            //Logger.getLogger("log").log(USR.EXTRA, "T");
            NetIF inter = nextUpdateIF_;

            if (inter == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "No table to send");

                return;
            }
            synchronized (inter) {
                long now = System.currentTimeMillis();

                byte[] table;
                synchronized (table_) {
                    table = table_.toBytes();
                }

                if (table[0] != 'T') {
                    Logger.getLogger("log").logln(USR.ERROR, leadin()+"Routing table does not start with 'T'");
                    throw new Error("Bad Routing Table");
                }

                // 25052012 sclayman
                Logger.getLogger("log").logln(1<<6, leadin() + now+ " Sending table to interface "+ inter);
                Logger.getLogger("log").logln(1<<6,
                                              leadin() + "\nsize " + table.length + " = 5+" +
                                              (table.length-5) + " -> " + table_.showTransmitted());


                Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, table);
                datagram.setSrcAddress(getAddress());
                datagram.setDstAddress(inter.getRemoteRouterAddress());
                try {
                    sendDatagram(datagram);
                } catch (NoRouteToHostException e) {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Cannot send routing table datagram -- no route");
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "datagram = " + datagram);

                }

                lastTableUpdateTime_.put(inter, now);
                nextTableUpdateTime_.put(inter, now+options_.getMaxNetIFUpdateTime());
                //Logger.getLogger("log").logln(USR.ERROR, "Next table update time"+nextUpdateTime_);
            }
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

            RouterController controller = fabric.getRouter().getRouterController();

            return controller.getName() + " " + RF;
        }

    }

}
