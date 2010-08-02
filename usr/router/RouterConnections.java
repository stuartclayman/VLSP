package usr.router;

import java.util.Scanner;
import java.net.*;
import java.nio.channels.*;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * A RouterConnections accepts new connections from exisiting routers.
 */
public class RouterConnections implements Runnable {
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
    public RouterConnections(RouterController cont, int port) {
        controller = cont;
        this.port = port;
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

            System.out.println("R2R: Listening on port: " + port);

            System.err.println("R2R: Ready to accept on " + serverSocket);


            myThread = new Thread(this, "RouterConnections" + hashCode());
            running = true;
            myThread.start();

            return true;
        }
	catch (IOException ioe) {
            System.err.println("R2R: cannot listen on port: " + port);
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
    public void run() {
        while (running) {
            try {
                Socket local = serverSocket.accept();

                System.err.println("R2R: Did accept on: " + serverSocket);

                System.out.println("R2R: newConnection: " + local);

                InetSocketAddress refAddr = new InetSocketAddress(local.getInetAddress(), local.getPort());
                //System.err.println("RouterConnections => " + refAddr + " # " + refAddr.hashCode());


                NetIF netIF = new TCPNetIF(local);
                netIF.setID(refAddr.hashCode());

                System.out.println("R2R: netif = " + netIF);

                controller.addNetIF(netIF);

            } catch (IOException ioe) {
                System.err.println("R2R: accept failed");
            }

        }
        
    }

}
