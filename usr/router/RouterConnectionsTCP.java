package usr.router;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;

import usr.common.TimedThread;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.TCPEndPointSrc;
import usr.net.TCPEndPointDst;

/**
 * A RouterConnections accepts new connections from exisiting routers,
 * using TCP as the router to router connection mechanism.
 */
public class RouterConnectionsTCP implements RouterConnections, Runnable {
    // The RouterController
    RouterController controller;

    // the port this router is listening on
    int port;

    // A Server socket
    ServerSocketChannel channel = null;
    ServerSocket serverSocket = null;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    /**
     * Construct a RouterConnections, given a specific port.
     */
    public RouterConnectionsTCP(RouterController cont, int port) {
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
        try {
            // WAS: serverSocket = new ServerSocket(port);
            channel = ServerSocketChannel.open();
            //channel.configureBlocking(true);
            serverSocket = channel.socket();
            serverSocket.bind(new InetSocketAddress(port));

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Listening on port: " + port);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Ready to accept on " + serverSocket);


            myThread = new TimedThread(controller.getThreadGroup(), this, "/" + controller.getName() + "/RouterConnections/" + hashCode());
            running = true;
            myThread.start();

            return true;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot listen on port: " + port);
            return false;
        }

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
        int failCount = 0;
        
        while (running) {
            try {
                // Set up a new end point
                TCPEndPointDst dst = new TCPEndPointDst(serverSocket);
                NetIF netIF = new TCPNetIF(dst, controller.getListener());
                //netIF.setName("RouterConnections");

                // this connect() waits (actually does an accept() ) 
                // by waiting for an incoming connect() from another router
                netIF.connectPhase1();

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "newConnection: " + dst.getSocket());


                int netIFHashCode = getLocalHashCode(netIF);


                Logger.getLogger("log").logln(USR.STDOUT, "RouterConnections hashCode => " + " # " + netIFHashCode);


                netIF.setID(netIFHashCode);

                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "netif = " + netIF);

                // The following method is latched, awaiting a getTemporaryNetIFByID() call
                controller.registerTemporaryNetIFIncoming(netIF);

                if (running) {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + " NetIF: " + netIF.getLocalAddress() + ":" + netIF.getLocalPort() +  " <-> " + netIF.getInetAddress() + ":" + netIF.getPort() );

                    netIF.connectPhase2();
                }
                
            } catch (IOException ioe) {
                // only print if running, not when stopping
                if (running) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "accept failed");

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
     * Create an EndPoint source for a TCPNetIF
     */
    public NetIF getNetIFSrc(String host, int connectionPort) throws UnknownHostException, IOException {
            TCPEndPointSrc src = new TCPEndPointSrc(host, connectionPort);
            NetIF netIF = new TCPNetIF(src, controller.getListener());

            return netIF;
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
     * Create the String to print out before a message
     */
    String leadin() {
        final String R2R = "R2R-TCP: ";

        if (controller == null) {
            return R2R;
        } else {
            return controller.getName() + " " + R2R;
        }

    }

}
