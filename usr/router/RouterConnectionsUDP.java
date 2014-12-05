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
        // initialise the socket
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
        while (running) {
            try {

                DatagramChannel channel = DatagramChannel.open();
                //channel.configureBlocking(true);
                DatagramSocket socket = channel.socket();
                socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));

                port = socket.getLocalPort();

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Listening on port: " + port);


                UDPEndPointDst dst = new UDPEndPointDst(socket);
                NetIF netIF = new UDPNetIF(dst, controller.getListener());
                netIF.setName("RouterConnections");

                // We dont do a connect in RouterConnectionsUDP
                // This is handled when we do a setRemoteAddress() in INCOMING_CONNECTION
                //netIF.connect();


                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "newConnection: " + dst.getSocket());

                int netIFHashCode = getLocalHashCode(netIF);


                Logger.getLogger("log").logln(USR.STDOUT, "RouterConnections hashCode => " + " # " + netIFHashCode);

                netIF.setID(netIFHashCode);

                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "netif = " + netIF);

                // The following method is latched, awaiting a getTemporaryNetIFByID() call
                controller.registerTemporaryNetIFIncoming(netIF);

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + " NetIF: " + netIF.getLocalAddress() + ":" + netIF.getLocalPort() +  " <-> " + netIF.getInetAddress() + ":" + netIF.getPort() );
                

            } catch (IOException ioe) {
                // only print if running, not when stopping
                if (running) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "accept failed");
                }
            }

        }

    }

    /**
     * Return an hash code for locally created NetIF.
     */
    public int getLocalHashCode(NetIF netIF) {
        InetSocketAddress refAddr = new InetSocketAddress(netIF.getLocalAddress(), netIF.getLocalPort());

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "getLocalHashCode -> " + refAddr);

        return refAddr.hashCode();
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
        final String R2R = "R2R: ";

        if (controller == null) {
            return R2R;
        } else {
            return controller.getName() + " " + R2R;
        }

    }

}
