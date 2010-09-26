package usr.router;

import usr.net.TCPEndPointSrc;
import usr.net.GIDAddress;
import usr.net.Datagram;
import usr.protocol.Protocol;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.IOException;

/**
 * The AppSocketMux class allocates pseudo sockets as as application
 * layer function in order that applications can send data to each other
 * and have an address and a port.
 */
public class AppSocketMux implements NetIFListener {
    // the RouterController
    RouterController controller;

    // The NetIF to the Router fabric
    NetIF netIF;

    // the count of the no of Datagrams
    int datagramCount = 0;

    // the next free port
    int freePort = 32768;

    // The list of all AppSockets
    HashMap<Integer, AppSocket>socketMap;

    // The queues of Datagrams for all AppSockets
    HashMap<Integer, LinkedBlockingQueue<Datagram>>socketQueue;

    /**
     * Construct an AppSocketMux.
     */
    AppSocketMux(RouterController controller) {
        this.controller = controller;
        socketMap = new HashMap<Integer, AppSocket>();
        socketQueue = new HashMap<Integer, LinkedBlockingQueue<Datagram>>();

    }


    /**
     * Connect to my local Router.
     */
    boolean connect() {
        try {
            // initialise socket
            //  Can't use InetAddress.getLocalHost() - might need to fix this
            TCPEndPointSrc src = new TCPEndPointSrc("localhost", controller.getConnectionPort());
        
            netIF = new TCPNetIF(src);
            netIF.setAddress(new GIDAddress(0));
            netIF.connect();
            
            System.err.println(leadin() + "Connected to: " + "localhost:" + controller.getConnectionPort());

            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }

            // now we need to plug the other end into a port
            Socket socket = src.getSocket();
            // first create an address from the same host, but
            // using the passed in port number
            // and create the reference address
            InetSocketAddress refAddr = new InetSocketAddress(socket.getInetAddress(), socket.getLocalPort());
            System.err.println(leadin() + "refAddr " + refAddr + " # " + refAddr.hashCode());

            // find the controller side NetIF
            NetIF tempNetIF = controller.getTemporaryNetIFByID(refAddr.hashCode());
            tempNetIF.setName("localnet");
            tempNetIF.setAddress(new GIDAddress(0));
            tempNetIF.setRemoteRouterName(controller.getName());
            tempNetIF.setRemoteRouterAddress(new GIDAddress(0));

            // now plug it in to the Router Fabric
            controller.plugTemporaryNetIFIntoPort(tempNetIF);

            // tell the NetIF, this is the listener
            netIF.setNetIFListener(this);

            return true;

        } catch (UnknownHostException uhe) {
            return false;
        } catch (IOException ioe) {
            return false;
        }

    }

    /**
     * Close all sockets.
     */
    public boolean stop() {
        HashSet<AppSocket> sockets = new HashSet<AppSocket>(socketMap.values());

        for (AppSocket s : sockets) {
            s.close();
        }

        netIF.close();

        return true;
    }

    /**
     * Add an AppSocket.
     */
    void addAppSocket(AppSocket s) {
        int port = s.getLocalPort();

        System.err.println(leadin() + "addAppSocket " + port + "  -> " + s);


        // register the socket
        socketMap.put(port, s);

        // set up the incoming queue
        socketQueue.put(port, new LinkedBlockingQueue<Datagram>());
    }

    /**
     * Remove an AppSocket.
     */
    void removeAppSocket(AppSocket s) {
        int port = s.getLocalPort();

        System.err.println(leadin() + "removeAppSocket " + port + "  -> " + s);

        // unregister the socket
        socketMap.remove(port);

        // remove the queue
        socketQueue.remove(port);

        // TODO: free up port number for reuse
    }

    /**
     * Is a specified port number available
     */
    boolean isPortAvailable(int port) {
        // visit each socket and get its port
        for (AppSocket s : socketMap.values()) {
            if (port == s.getLocalPort()) {
                // the port is in use
                return false;
            }
        }

        // no one is using this port no
        return true;
    }


    /**
     * Find the next free port number.
     */
    int findNextFreePort() {
        // check if the next one is actually not free
        while (! isPortAvailable(freePort)) {
            freePort++;
        }

        // return freePort and skip to next one
        return freePort++;
    }

    /**
     * A NetIF has a datagram.
     */
    public boolean datagramArrived(NetIF netIF) {
        Datagram datagram= netIF.readDatagram();

        datagramCount++;

        if (datagram.getProtocol() == Protocol.CONTROL) {
            byte[] payload = datagram.getPayload();
            byte controlChar= payload[0];

            if (controlChar == 'C') {
                System.err.println(leadin() + "Got Close");
                stop();
            }

            return true;
        } else {

            System.err.println(leadin() + datagramCount + " GOT DATAGRAM from "  + " = " + datagram.getSrcAddress() + ":" + datagram.getSrcPort() + " => " + datagram.getDstAddress() + ":" + datagram.getDstPort());

            // check the port of the socket and send it on
            int dstPort = datagram.getDstPort();

            // find the socket to deliver to
            AppSocket socket = socketMap.get(dstPort);

            if (socket != null) {
                // System.err.println(leadin() + "About to queue for " + socket);
                LinkedBlockingQueue<Datagram> queue = getQueueForPort(dstPort);
                queue.add(datagram);

                return true;
            } else {
                System.err.println(leadin() + "Cant deliver to port " + dstPort);
                return false;
            }
        }
    }
    
    /**
     * Send a datagram to the router fabric.
     */
    public boolean sendDatagram(Datagram dg) {
        // patch up the source address in the Datagram
        GIDAddress srcAddr = controller.getAddress();
        dg.setSrcAddress(srcAddr);

        return netIF.forwardDatagram(dg);
    }

    /**
     * Get the queue for a specified port.
     */
    LinkedBlockingQueue<Datagram> getQueueForPort(int port) {
        return socketQueue.get(port);
    }

    /**
     * A NetIF is closing.
     */
    public boolean netIFClosing(NetIF netIF) {
        return true;
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String AS = "ASM: ";

        return controller.getName() + " " + AS;
    }


}
