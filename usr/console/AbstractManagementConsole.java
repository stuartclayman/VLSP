package usr.console;

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
 * for doing component management.
 */
public abstract class AbstractManagementConsole implements ManagementConsole, Runnable {

    // the port this router is listening on
    int port;
    boolean theEnd= false;
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

    // A queue of requests
    BlockingQueue<Request> requestQueue;

    // The Thread
    Thread myThread;

    // are we running
    boolean running = false;

    // The Finite State Machine
    FSMState fsm;

    /**
     * The no arg Constructor.
     */
    public AbstractManagementConsole() {
    }

    /**
     * Initialise a ManagementConsole, given a specific port.
     */

    public void initialise (int port) {
        this.port = port;
        requestQueue = new LinkedBlockingQueue<Request>();
        channelKeys = new HashMap<SocketChannel, SelectionKey>();

        // FSM
        fsm = FSMState.STATE0;

        // setp the Commands
        commandMap = new HashMap<String, Command>();

        
        registerCommands();
    }
    
    /**
     * Register a new command with the ManagementConsole.
     */
    public void register(Command command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);
    }

    /**
     * Register the relevant commands for the ManagementConsole.
     * The actual ones are defined by the concrete implementations.
     */
    public abstract void  registerCommands();


    /**
     * Get a handle on the queue
     */
    public BlockingQueue<Request> queue() {
        return requestQueue;
    }

    /**
     * Add a Request to the queue
     */
    public BlockingQueue<Request> addRequest(Request q) {
        requestQueue.add(q);
        return requestQueue;
    }



    /**
     * Start the listener.
     * We use a Selector to process network input.
     * This uses select() in the underlying system
     * and serializes input.
     */
    public boolean start() {
        // check the UnknownCommand exists
        Command unknown = commandMap.get("__UNKNOWN__");

        if (unknown == null) {
            System.err.println(leadin() + "the UnknownCommand has not been registered");
            return false;
        }

	// initialise the socket
        try {
            // Create a non-blocking server socket channel on port
            channel = ServerSocketChannel.open();
            serverSocket = channel.socket();
            channel.configureBlocking(false);
            serverSocket.bind(new InetSocketAddress(port));



            System.out.println(leadin() + "Listening on port: " + port);

            boolean ready = setUp();

            // FSM
            fsm = FSMState.START;


            myThread = new Thread(this, this.getClass().getName() + "-" + hashCode());
            running = true;
            myThread.start();

            return ready;
        }
	catch (IOException ioe) {
            System.err.println(leadin() + "Cannot listen on port: " + port);
            return false;
        }

    }
    
    /**
     * Stop the listener.
     */
    public boolean stop() {
        try {
            running = false;

            // call Cleardown
            boolean cleardown = clearDown();

            // FSM
            fsm = FSMState.STOP;

            // interrupt any waits
            myThread.interrupt();


            /* join too dangerous
            // wait for the thread to end
            // wait for myself
            try {
                myThread.join();
            } catch (InterruptedException ie) {
                // System.err.println("RouterController: stop - InterruptedException for myThread join on " + myThread);
            }
            */

            // wait for the thread to end
            waitFor();


            return cleardown;

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
                // FSM
                fsm = FSMState.SELECTING;

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
                        System.out.println(leadin()+"Processing keys");
                        
                        if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                            // do we have an accept

                            // FSM
                            fsm = FSMState.CONNECTING;

                            // Accept the incoming connection.
                            Socket local = serverSocket.accept();
                            
                            System.out.println(leadin() + "Did accept on: " + serverSocket);
                            // Deal with incoming connection
                            newConnection(local);

                        } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                            // do we have a read

                            // FSM
                            fsm = FSMState.PROCESSING;

                            // get the channel
                            SocketChannel sc = (SocketChannel)key.channel();

                            // and process some input from it
                            processInput( sc );
                        } else {
                            System.err.println(leadin() + "Unexpected behaviour on " + key);
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

        // System.out.println(leadin() + " End of thread " +  Thread.currentThread());
        // notify we have reached the end of this thread
        theEnd();
        
        System.out.println(leadin() + "end");       

    }

    /**
     * Wait for this thread.
     */
    private synchronized void waitFor() {
        System.out.println(leadin() + "waitFor");
        setTheEnd();
        try {
            wait();
        } catch (InterruptedException ie) {
        }
    }
    
    /**
     * Notify this thread.
     */
    private synchronized void theEnd() {
        System.out.println(leadin() + "theEnd");
        while (!ended()) {
            try {
                System.out.println(leadin()+"In a loop");
                Thread.sleep(100);
            } catch (Exception e) {
            
            }
        }
        notifyAll();
    }

    synchronized void setTheEnd() {
        theEnd= true;
    }

    synchronized boolean ended() {
      return theEnd;  
    }

  

    /**
     * Set up.
     */
    protected boolean setUp() {
        try {
            selector = Selector.open();

            // get channel for main socket
            ServerSocketChannel ssc = serverSocket.getChannel();

            // check channel
            if (ssc == null) {
                System.err.println(leadin() + "no channel for " + serverSocket);
                return false;
            } else {
                // set channel to be non-blocking
                // already done earlier
                // ssc.configureBlocking(false);

                // System.err.println(leadin() + "Registering " + ssc);

                // register this channel with the selector
                SelectionKey key = ssc.register( selector, SelectionKey.OP_ACCEPT );

                System.out.println(leadin() + "Ready to accept on " + serverSocket);
                return true;
            }
        } catch (IOException ioe) {
            // cant process socket, so
                System.err.println(leadin() + "IOException on channel for " + serverSocket);
                return false;            
        }
    }

    /**
     * Cleardown
     */
    protected boolean clearDown() {
        System.out.println(leadin() + "Cleardown");

        try {
            // close main socket
            serverSocket.close();
            System.out.println(leadin() + "Stopped listening on port: " + port);

            return true;
        } catch (Exception e) {
            System.err.println(leadin() + "Cannot close socket on port: " + port);
            return false;
        }
    }



    /**
     * Start reading from a new connection.
     */
    protected void newConnection(Socket s) {
        System.out.println(leadin() + "newConnection: " + s);

        SocketChannel sc;
        try {
            // Make sure channel for new connection is nonblocking
            sc = s.getChannel();
            sc.configureBlocking( false );
            System.out.println(leadin() + "Registering " + sc);
            // Register it with the Selector, for reading.
            SelectionKey key = sc.register( selector, SelectionKey.OP_READ );

            //System.err.println("selector keys = " + selector.keys());

            channelKeys.put(sc, key);

            //System.err.println("channelKeys = " + channelKeys);

        } catch (IOException ioe) {
            System.err.println(leadin() + "Error on socket " + s);
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
        System.out.println(leadin() + "endConnection: " + c.socket());

        unregisterChannel(c);

        // close a remote connection
        try {
            c.close();
        } catch (IOException ioe) {
            System.err.println(leadin() + "CANNOT close : " + c.socket());
        }
    }

    /**
     * Unregister a channel from the Selector.
     */
    protected void unregisterChannel(SocketChannel c) {
        // find the SelectionKey for this channel
        SelectionKey key = channelKeys.get(c);

        // Remove it from the Selector
        key.cancel();

        //System.err.println(leadin() + "Selector keys = " + selector.keys());

        channelKeys.remove(c);
    }

    /**
     * Process some input.
     * This reads everything that is available, and processes it immediately.
     * This does not deal with end of line or end of input delineator.
     */
    protected void processInput(SocketChannel sc) {
        int read = -1;

        // read some data
        try {
            // clear buffer
            buffer.clear();
            // read into buffer
            read = sc.read(buffer);
        } catch (IOException ioe) {
            // bad error
            endConnection(sc);
            return;
        }

        // check what was read
        if (read != -1) {
            // set buffer ready
            buffer.flip();

            // convert buffer to string
            String value = charset.decode(buffer).toString().trim();

            // inform the input handler
            // with the value and the channel the input came from
            boolean result = handleInput(value, sc);

            if (result == false) {
                // there was an error in responding down the channel
                // TODO:  decide what to do
            }

        } else {
            // EOF for the connection
            endConnection(sc);
        }
    }



    /**
     * Handle the input.
     * Commands are split into 2 kinds:
     * i) synchronous, which return values immediately, and
     * ii) asynchronous, which return immediately, and operate some time later
     * Returns false if there is a problem responding down the channel
     */
    protected boolean handleInput(String value, SocketChannel sc) {
        System.out.println(leadin() + ">>> " + value);

        if (value == null && value.length() == 0) {
            // empty - do nothing
            return true;
        } else {
            // find the command
            int endOfCommand = value.indexOf(' ');
            String commandName;

            // get the command from the input
            if (endOfCommand == -1) {
                // if there is no space the whole input is the command name
                commandName = value;
            } else {
                // from 0 to first space
                commandName = value.substring(0, endOfCommand);
            }

            // now lookup the command
            Command command = commandMap.get(commandName);
            boolean result;

            if (command != null) {
                // we got a command
            } else {
                //fetch the UnknownCommand
                command = commandMap.get("__UNKNOWN__");
            }

            // so bind the command to the channel
            command.setChannel(sc);
            // and evaluate the input
            result = command.evaluate(value);

            return result;

        }
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String MC = "MC: ";
        ComponentController control = getComponentController();

        if (control == null) {
            return MC;
        } else {
            return control.getName() + " " + MC;
        }

    }


}

