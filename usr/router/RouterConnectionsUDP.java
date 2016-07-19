package usr.router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;

import usr.common.TimedThread;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.UDPEndPointSrc;
import usr.net.UDPEndPointDst;

/**
 * A RouterConnections accepts new connections from exisiting routers,
 * using UDP as the router to router connection mechanism.
 */
public class RouterConnectionsUDP implements RouterConnections, Runnable {
    // The RouterController
    RouterController controller;

    // the port this router is listening on
    int port;


    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    /**
     * Construct a RouterConnections, given a specific port.
     */
    public RouterConnectionsUDP(RouterController cont, int port) {
        controller = cont;
        this.port = port;
    }

    /**
     * Get the port for the connection port
     */
    public int getConnectionPort() {
        return port;
    }

    /**
     * Start the connections listening.
     */
    public boolean start() {
        myThread = new TimedThread(controller.getThreadGroup(), this, "/" + controller.getName() + "/RouterConnections/" + hashCode());
        running = true;
        myThread.start();

        return true;
    }

    /**
     * Stop the listener.
     */
    public boolean stop() {
        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "stop");

            running = false;
            myThread.interrupt();

            return true;

        } catch (Exception e) {
            return false;
        }

    }

    /**
     * The main thread loop.
     */
    @Override
    public void run() {
        int failCount = 0;
        DatagramSocket socket = null;
        InetSocketAddress address = null;
        
        while (running) {
            try {
                // Need a new socket each time for UDP
                // There is no equivalent of TCP accept

                // Listen on all interfaces
                address = new InetSocketAddress(InetAddress.getLocalHost(), 0);
                socket = new DatagramSocket(address);

                // Set the port for the next incoming call
                port = socket.getLocalPort();

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Listening on port: " + port);

                // Set up a new end point
                UDPEndPointDst dst = new UDPEndPointDst(socket);
                NetIF netIF = new UDPNetIF(dst, controller.getListener());
                //netIF.setName("RouterConnections");

                // We dont do a real connect in RouterConnectionsUDP
                // This is handled when we do a setRemoteAddress() in INCOMING_CONNECTION
                netIF.connectPhase1();


                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "newConnection: " + dst.getSocket());

                int netIFHashCode = getLocalHashCode(netIF);


                Logger.getLogger("log").logln(USR.STDOUT, "RouterConnections hashCode => " + " # " + netIFHashCode);

                netIF.setID(netIFHashCode);

                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "netif = " + netIF);

                // The following method is latched, awaiting a getTemporaryNetIFByID() call
                controller.registerTemporaryNetIFIncoming(netIF);

                // only continue if running
                if (running) {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + " NetIF: " + netIF.getLocalAddress() + ":" + netIF.getLocalPort() +  " <-> " + netIF.getInetAddress() + ":" + netIF.getPort() );
                
                    netIF.connectPhase2();
                } else {
                    break;
                }

            } catch (IOException ioe) {
                // only print if running, not when stopping
                if (running) {
                    //ioe.printStackTrace();
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "socket failed " + ioe.getMessage() + " for socket " + address);

                    failCount++;

                    if (failCount == 10) {
                        // too many failures - bomb out
                        stop();
                        controller.informFailure();
                    }
                }
            }

        }

    }

    /**
     * Return an hash code for locally created NetIF.
     */
    public int getLocalHashCode(NetIF netIF) {
        try {
            // Hash code is a function of the local host IP
            InetSocketAddress refAddr = new InetSocketAddress(InetAddress.getLocalHost(), netIF.getLocalPort());
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "getLocalHashCode -> " + refAddr);

            return refAddr.hashCode();
        } catch (UnknownHostException uhe) {
            // If local host addr is not available then
            // Hash code is a function of the NetIF local address
            InetSocketAddress refAddr = new InetSocketAddress(netIF.getLocalAddress(), netIF.getLocalPort());
            return refAddr.hashCode();
        }
    }

    /**
     * Return a hash code for a NetIF in Create Connection.
     */
    public int getCreateConnectionHashCode(NetIF netIF, InetAddress addr, int port) {
        // get an InetSocketAddress
        InetSocketAddress refAddr = new InetSocketAddress(addr, port);

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "getCreateConnectionHashCode -> " + refAddr);

        return refAddr.hashCode();
    }


    /**
     * Create an EndPoint source for a UDPNetIF
     */
    public NetIF getNetIFSrc(String host, int connectionPort) throws UnknownHostException, IOException {
            UDPEndPointSrc src = new UDPEndPointSrc(host, connectionPort);
            NetIF netIF = new UDPNetIF(src, controller.getListener());

            return netIF;
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String R2R = "R2R-UDP: ";

        if (controller == null) {
            return R2R;
        } else {
            return controller.getName() + " " + R2R;
        }

    }

}
