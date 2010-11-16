package usr.console;

import usr.net.Address;
import usr.logging.*;
import usr.protocol.MCRP;
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

    // are we running
    volatile boolean running = false;
    
    // A Server socket
    ServerSocketChannel channel = null;
    ServerSocket serverSocket = null;

    // A network Selector
    Selector selector;

    // read data from channel into the buffer
    ByteBuffer buffer = ByteBuffer.allocate(4096);
    StringBuilder readString= new StringBuilder(4096);

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
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "the UnknownCommand has not been registered");
            return false;
        }

	// initialise the socket
        try {
            // Create a non-blocking server socket channel on port
            channel = ServerSocketChannel.open();
            serverSocket = channel.socket();
            channel.configureBlocking(false);
            serverSocket.bind(new InetSocketAddress(port));



            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Listening on port: " + port);

            boolean ready = setUp();

            // FSM
            fsm = FSMState.START;


            myThread = new Thread(this, this.getClass().getName() + "-" + hashCode());
            running = true;
            myThread.start();

            return ready;
        }
	catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot listen on port: " + port);
            return false;
        }

    }
    
    /** shutDown should be called from external threads */
    public void shutDown() {
        stop();
        myThread.interrupt();
        try {
            myThread.join();
        } catch (InterruptedException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + " console shutDown method interrupted.");
        }
    }
    
    /**
     * Stop the listener -- this should be called by shut down commands from within the thread 
     of the console
     */
    public synchronized boolean stop() {
        if (!running) {
            System.err.println("CALLED TWICE");
            return true;
        }
        boolean cleardown= true;
       
        running = false;

            // call Cleardown
        cleardown= clearDown();
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+" exits cleardown");
            // FSM
        fsm = FSMState.STOP;

            // interrupt any waits
        

        return cleardown;      
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
              //  System.err.println("Enter select");
                int num = selector.select();
               // System.err.println("Selected");
                // did select() return with no values ?
                if (num == 0) {
                    // go again
                    continue;
                } else {
                    // check what is ready

                    Set<SelectionKey> keys = selector.selectedKeys();

                    for (SelectionKey key : keys) {
                       // System.err.println("Selecting "+key);
                        
                        if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                            // do we have an accept

                            // FSM
                            fsm = FSMState.CONNECTING;

                            // Accept the incoming connection.
                            Socket local = serverSocket.accept();
                            
                            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Did accept on: " + serverSocket);
                            // Deal with incoming connection
                            newConnection(local);

                        } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                            // do we have a read
                            //  System.err.println("Processing "+key);
                            // FSM
                            fsm = FSMState.PROCESSING;

                            // get the channel
                            SocketChannel sc = (SocketChannel)key.channel();

                            // and process some input from it
                            processInput( sc );
                        } else {
                            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unexpected behaviour on " + key);
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
        
         Logger.getLogger("log").logln(USR.STDOUT, leadin() + " End of thread " +  Thread.currentThread());
        // notify we have reached the end of this thread
      //  System.err.println("End of thread");
 
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "end");       

    }

    /**
     * Is the ManagementConsole ready selecting.
     */
    public boolean isSelecting() {
        return fsm == FSMState.SELECTING;
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
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "no channel for " + serverSocket);
                return false;
            } else {
                // set channel to be non-blocking
                // already done earlier
                // ssc.configureBlocking(false);

                // Logger.getLogger("log").logln(USR.ERROR, leadin() + "Registering " + ssc);

                // register this channel with the selector
                SelectionKey key = ssc.register( selector, SelectionKey.OP_ACCEPT );

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Ready to accept on " + serverSocket);
                return true;
            }
        } catch (IOException ioe) {
            // cant process socket, so
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "IOException on channel for " + serverSocket);
                return false;            
        }
    }

    /**
     * Cleardown
     */
    protected boolean clearDown() {
        // Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Cleardown");

        try {
            // close main socket
            serverSocket.close();
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopped listening on port: " + port);

            return true;
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot close socket on port: " + port);
            return false;
        }
    }



    /**
     * Start reading from a new connection.
     */
    protected void newConnection(Socket s) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "newConnection: " + s);

        SocketChannel sc;
        try {
            // Make sure channel for new connection is nonblocking
            sc = s.getChannel();
            sc.configureBlocking( false );
            // Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Registering " + sc);
            // Register it with the Selector, for reading.
            SelectionKey key = sc.register( selector, SelectionKey.OP_READ );

            //Logger.getLogger("log").logln(USR.ERROR, "selector keys = " + selector.keys());

            channelKeys.put(sc, key);

            //Logger.getLogger("log").logln(USR.ERROR, "channelKeys = " + channelKeys);

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Error on socket " + s);
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
        // Logger.getLogger("log").logln(USR.STDOUT, leadin() + "endConnection: " + c.socket());

        unregisterChannel(c);

        // close a remote connection
        try {
            c.close();
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "CANNOT close : " + c.socket());
        }
    }

    /**
     * Unregister a channel from the Selector.
     */
    protected void unregisterChannel(SocketChannel c) {
        // find the SelectionKey for this channel
        SelectionKey key = channelKeys.get(c);

        // Remove it from the Selector
        if (key == null)
            return;
        key.cancel();

        //Logger.getLogger("log").logln(USR.ERROR, leadin() + "Selector keys = " + selector.keys());

        channelKeys.remove(c);
    }

    /**
     * Process some input.
     * This reads everything that is available, and processes it immediately.
     * This does not deal with end of line or end of input delineator.
     */
    protected void processInput(SocketChannel sc) {
        
        int read = -1;
        int index= readString.indexOf("\n");
        if (index != -1) {
          //  System.err.println("Index = "+index);
            String value = readString.substring(0,index);
          //  System.err.println("Found string "+value);
            readString.delete(0,index+1);
            
            handleInput(value, sc);
            return;            
        }
        // read some data
        while (true) {
            buffer.clear();
            
            try {

                // read into buffer
                read = sc.read(buffer);
            } catch (IOException ioe) {
                // bad error
               // System.err.println("IO ERROR");
                endConnection(sc);
                return;
            }
            if (read == -1) {
              ///  System.err.println("No characters");
                endConnection(sc);
            }
            buffer.flip();
            readString.append(charset.decode(buffer).toString());
            index= readString.indexOf("\n"); // look for the /n character
            if (index == -1)  // No \n we need to read some more
                continue;  
           // System.err.println( "Index = "+index);
            String value = readString.substring(0,index);
            //System.err.println("Found string "+value);
           // System.err.println("Read string "+value);
           // System.err.println("String was "+readString);
            readString.delete(0,index+1);
           // System.err.println("String is "+readString);
            handleInput(value, sc);
            break;
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
        int amount = value.length() > 131 ? 131 : value.length();
        String printable = value.substring(0, amount).replaceAll("\n", " ");

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + ">>> " + printable);

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
            try {
                result = command.evaluate(value);

                return result;

            } catch (Exception e) {
                // try and send generic error code
                respond(sc, MCRP.ERROR.CODE + " Error Exception " + e + " in " + value);
                return false;
            }


        }
    }

    /**
     * Respond with a given string.
     * Returns false if it cannot send the response down the channel.
     */
    private void respond(SocketChannel channel, String message) {
        message = message.concat("\n");

        try {
            channel.write(ByteBuffer.wrap(message.getBytes()));
        } catch (IOException ioe) {
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

