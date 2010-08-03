package usr.router;

import usr.net.Address;
import usr.net.IPV4Address;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.Charset;

/**
 * A ManagementConsole listens for connections
 * for doing router management.
 */
public class ManagementConsole implements Runnable {
    // The RouterController 
    RouterController controller;

    // the port this router is listening on
    int port;
    
    // A queue of requests
    BlockingQueue<Request> requestQueue;

    // A Server socket
    ServerSocketChannel channel = null;
    ServerSocket serverSocket = null;

    // A network Selector
    Selector selector;

    // read data from channel into the buffer
    ByteBuffer buffer = ByteBuffer.allocate(4096);

    // default Charset
    Charset charset = Charset.forName("UTF-8");

    // HashMap of SocketChannel -> SelectionKey
    HashMap<SocketChannel, SelectionKey> channelKeys;

    // HashMap of command name -> Command
    HashMap<String, Command> commandMap;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    /**
     * Construct a ManagementConsole, given a specific port.
     */
    public ManagementConsole(RouterController cont, int port) {
        controller = cont;
        this.port = port;
        requestQueue = new LinkedBlockingQueue<Request>();
        channelKeys = new HashMap<SocketChannel, SelectionKey>();
        commandMap = new HashMap<String, Command>();
    }

    /**
     * Get the RouterController this is a ManagementConsole for.
     */
    public RouterController getRouterController() {
        return controller;
    }

    /**
     * Get a handle on the queue
     */
    BlockingQueue<Request> queue() {
        return requestQueue;
    }

    /**
     * Start the listener.
     * We use a Selector to process network input.
     * This uses select() in the underlying system
     * and serializes input.
     */
    public boolean start() {
	// initialise the socket
        try {
            // Create a non-blocking server socket channel on port
            channel = ServerSocketChannel.open();
            serverSocket = channel.socket();
            channel.configureBlocking(false);
            serverSocket.bind(new InetSocketAddress(port));



            System.err.println("MC: Listening on port: " + port);

            boolean ready = setUp();


            myThread = new Thread(this, "ManagementConsole" + hashCode());
            running = true;
            myThread.start();

            return ready;
        }
	catch (IOException ioe) {
            System.err.println("MC: Cannot listen on port: " + port);
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


            boolean shutdown = shutDown();

            return shutdown;

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
                // select() on all channels
                int num = selector.select();

                // did select() return with no values ?
                if (num == 0) {
                    // go again
                    continue;
                } else {
                    // check what is ready

                    Set<SelectionKey> keys = selector.selectedKeys();

                    for (SelectionKey key : keys) {
                        if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                            // do we have an accept

                            // Accept the incoming connection.
                            Socket local = serverSocket.accept();

                            System.err.println("MC: Did accept on: " + serverSocket);
                            // Deal with incoming connection
                            newConnection(local);

                        } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                            // do we have a read
                            // get the channel
                            SocketChannel sc = (SocketChannel)key.channel();

                            // and process some input from it
                            processInput( sc );
                        } else {
                            System.err.println("MC: Unexpected behaviour on " + key);
                        }

                    }

                    // Remove the selected keys because you've dealt
                    // with them.
                    keys.clear();

                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        
        shutDown();
    }

    /**
     * Set up.
     */
    boolean setUp() {
        try {
            selector = Selector.open();

            // get channel for main socket
            ServerSocketChannel ssc = serverSocket.getChannel();

            // check channel
            if (ssc == null) {
                System.err.println("MC: no channel for " + serverSocket);
                return false;
            } else {
                // set channel to be non-blocking
                // already done earlier
                // ssc.configureBlocking(false);

                System.err.println("MC: Registering " + ssc);

                // register this channel with the selector
                SelectionKey key = ssc.register( selector, SelectionKey.OP_ACCEPT );

                System.err.println("MC: Ready to accept on " + serverSocket);
                return true;
            }
        } catch (IOException ioe) {
            // cant process socket, so
                System.err.println("MC: IOException on channel for " + serverSocket);
                return false;            
        }
    }

    /**
     * Shutdown
     */
    boolean shutDown() {
        System.err.println("MC: Shutdown");

        try {
            // close main socket
            serverSocket.close();
            System.err.println("MC: Stopped listening on port: " + port);
            return true;
        } catch (Exception e) {
            System.err.println("MC: Cannot close socket on port: " + port);
            return false;
        }
    }



    /**
     * Start reading from a new connection.
     */
    void newConnection(Socket s) {
        System.err.println("MC: newConnection: " + s);

        SocketChannel sc;
        try {
            // Make sure channel for new connection is nonblocking
            sc = s.getChannel();
            sc.configureBlocking( false );
            System.err.println("MC: Registering " + sc);
            // Register it with the Selector, for reading.
            SelectionKey key = sc.register( selector, SelectionKey.OP_READ );

            //System.err.println("selector keys = " + selector.keys());

            channelKeys.put(sc, key);

            //System.err.println("channelKeys = " + channelKeys);

        } catch (IOException ioe) {
            System.err.println("MC: Error on socket " + s);
            try {
                s.close();
            } catch (Exception e) {
            }
        }

    }

    /**
     * End a connection
     */
    public void endConnection(SocketChannel c) {
        System.err.println("MC: endConnection: " + c.socket());

        unregisterChannel(c);

        // close a remote connection
        try {
            c.close();
        } catch (IOException ioe) {
        }
    }

    /**
     * Unregister a channel from the Selector.
     */
    void unregisterChannel(SocketChannel c) {
        // find the SelectionKey for this channel
        SelectionKey key = channelKeys.get(c);

        // Remove it from the Selector
        key.cancel();

        //System.err.println("MC: Selector keys = " + selector.keys());

        channelKeys.remove(c);
    }

    /**
     * Process some input.
     * This reads everything that is available, and queues it immediately.
     * This does not deal with end of line or end of input delineator.
     */
    void processInput(SocketChannel sc) {
        int read = -1;

        // read some data
        try {
            buffer.clear();
            read = sc.read(buffer);
        } catch (IOException ioe) {
        }

        // check what was read
        if (read != -1) {
            buffer.flip();

            // convert buffer to string
            String value = charset.decode(buffer).toString().trim();

            // inform the input handler
            handleInput(value, sc);

        } else {
            // EOF for the connection
            endConnection(sc);
        }
    }

    /**
     * Respond to the client
     */
    void respond(SocketChannel sc, String s) throws IOException {
        s = s.concat("\n");
        System.err.print("MC: <<< RESPONSE: " + s);
        sc.write(ByteBuffer.wrap(s.getBytes()));
    }

    /**
     * Register a command.
     */
    void register(Command command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);
    }

    /**
     * Handle the input.
     * Commands are split into 2 kinds:
     * i) synchronous, which return values immediately, and
     * ii) asynchronous, which return immediately, and operate some time later
     * <p>
     * Synchronous commands include:
     * QUIT - end the connection
     * GET_NAME - get the name of the router
     * SET_NAME name - set the name of the router
     * GET_CONNECTION_PORT - get the port the router uses for router-to-router connections
     * INCOMING_CONNECTION connection_ID weight remote_port_no - information about a router-to-router connection 
     * LIST_CONNECTIONS - lists all known connections
     * <p>
     * Asynchronous commands include:
     * CREATE_CONNECTION ip_addr/port connection_ID - create a new network interface to
     * a router on the address ip_addr/port
     * 
     */
    protected void handleInput(String value, SocketChannel sc) {
        System.out.println("MC: >>> " + value);

        if (value == null && value.length() == 0) {
            // empty - do nothing
        } else {            
            // check for synchronous commands that return immediately
            if (value.equals("QUIT")) {   // check if the client is quitting

                try {
                    respond(sc, "500 QUIT");
                    endConnection(sc);
                } catch (IOException ioe) {
                    System.err.println("MC: QUIT failed");
                }

            } else if (value.equals("GET_NAME")) {
                try {
                    String name = controller.getName();
                    respond(sc, "201 " + name);
                } catch (IOException ioe) {
                    System.err.println("MC: GET_NAME failed");
                }

            } else if (value.startsWith("SET_NAME")) {
                try {
                    String name = value.substring(8).trim();
                    controller.setName(name);
                    respond(sc, "202 " + name);
                } catch (IOException ioe) {
                    System.err.println("MC: SET_NAME failed");
                }

            } else if (value.startsWith("SET_ADDRESS")) {
                try {
                    String rest = value.substring(11).trim();
                    String[] parts = rest.split(" ");
                    
                    if (parts.length == 3) {

                        String routerPortName = parts[0];
                        String type = parts[1];
                        String addr = parts[2];
                        Address address = null;
                        
                        // find port
                        String portNo = routerPortName.substring(4);
                        Scanner scanner = new Scanner(portNo);
                        int p = scanner.nextInt();
                        RouterPort routerPort = controller.getPort(p);

                        if (routerPort == null || routerPort == RouterPort.EMPTY) {
                            respond(sc, "403 SET_ADDRESS invalid port " + routerPortName);
                        }

                        // instantiate the address
                        if (type.toUpperCase().equals("IPV4")) {
                            try {
                                address = new IPV4Address(addr);
                            } catch (UnknownHostException uhe) {
                                respond(sc, "403 SET_ADDRESS UnknownHostException " + addr);
                            }
                        } else {
                            respond(sc, "403 SET_ADDRESS unknown type " + type);
                        }

                        // set address on netIF in port
                        NetIF netIF = routerPort.getNetIF();
                        netIF.setAddress(address);

                        respond(sc, "202 " + routerPortName);
                    } else {
                        respond(sc, "403 SET_ADDRESS wrong no of args ");
                    }
                } catch (IOException ioe) {
                    System.err.println("MC: SET_ADDRESS failed");
                }

            } else if (value.equals("GET_CONNECTION_PORT")) {
                try {
                    int port = controller.getConnectionPort();
                    respond(sc, "203 " + port);
                } catch (IOException ioe) {
                    System.err.println("MC: GET_CONNECTION_PORT failed");
                }

            } else if (value.equals("LIST_CONNECTIONS")) {
                try {
                    List<RouterPort> ports = controller.listPorts();
                    respond(sc, "204 " + "START");
                    int count = 0;
                    for (RouterPort rp : ports) {
                        if (rp.equals(RouterPort.EMPTY)) {
                            continue;
                        } else {
                            Address address = rp.netIF.getAddress();
                            String portString = " port" + rp.portNo + " " + rp.netIF.getName() + " " + rp.netIF.getRemoteRouterName() + " " + rp.netIF.getWeight() + " " + (address == null ? "No_Address" : address.toString());
                            respond(sc, (count + portString));
                            count++;
                        }               
                    }             
                    respond(sc, "204 " + "END");
                } catch (IOException ioe) {
                    System.err.println("MC: LIST_CONNECTIONS failed");
                }

            } else if (value.startsWith("INCOMING_CONNECTION")) {
                try {
                    String args = value.substring(19).trim();
                    String[] parts = args.split(" ");

                    if (parts.length == 4) {

                        String connectionID = parts[0];
                        String remoteRouterName = parts[1];
                        String weightStr = parts[2];
                        String remotePort = parts[3];

                        Scanner scanner;

                        // get remote port
                        scanner = new Scanner(remotePort);
                        int port;

                        try {
                            port = scanner.nextInt();
                        } catch (Exception e) {
                            respond(sc, "402 INCOMING_CONNECTION bad port number");
                            return;
                        }

                        // get connection weight
                        scanner = new Scanner(weightStr);
                        int weight = 0;

                        try {
                            weight = scanner.nextInt();
                        } catch (Exception e) {
                            respond(sc, "402 INCOMING_CONNECTION invalid value for weight");
                            return;
                        }


                        InetSocketAddress refAddr = new InetSocketAddress(sc.socket().getInetAddress(), port);
                        //System.err.println("ManagementConsole => " + refAddr + " # " + refAddr.hashCode());

                        /*
                         * Lookup netif and set its name
                         */
                        NetIF netIF = controller.getNetIFByID(refAddr.hashCode());

                        if (netIF != null) {
                            System.err.println("MC: Found NetIF " + netIF + " by id " + refAddr.hashCode());

                            // set its name
                            netIF.setName(connectionID);
                            // set its weight
                            netIF.setWeight(weight);
                            // set remote router
                            netIF.setRemoteRouterName(remoteRouterName);
                        
                            // now plug netIF into Router
                            controller.plugInNetIF(netIF);

                            respond(sc, "204 " +  connectionID);
                        } else {
                            respond(sc, "402 Cannot find NetIF for port " + port);
                        }
                    } else {
                         respond(sc, "402 INCOMING_CONNECTION wrong no of args ");
                    }
                } catch (IOException ioe) {
                    System.err.println("MC: INCOMING_CONNECTION failed");
                }

            } else if (value.startsWith("CREATE_CONNECTION")) {
                // it is an asynchronous command
                // and will be processed a bit later
                requestQueue.add(new Request(sc, value));
                System.err.println("MC: Requests = " + requestQueue);

            } else {
                // its an unknown command
                try {
                    respond(sc, "400 UNKNOWN " + value);
                } catch (IOException ioe) {
                    System.err.println("MC: Response to " + value + " failed");
                }       
            }
        }
    }

}
