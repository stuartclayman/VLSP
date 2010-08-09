// MCRPInteractor.java

package usr.interactor;

import usr.router.MCRP;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * This class implements the MCRP protocol for interacting
 * with the ManagementConsole of a Router.
 * It has methods for most requests in the MCRP protocol.
 * Most of the methods will throw an MCRPException if
 * somethnig is wrong.
 * Ones to look for are:
 *
 * <ul>
 * <li> MCRPNotReadyException: if this object is not ready to
 * accept a new request </li>
 * <li> MCRPNoConnectionException, if this object is not connected to
 * the router, probably after a quit(). </li>
 * </ul>
 */
public class MCRPInteractor {

    // The socket to the server
    Socket socket = null;
    Reader input = null;
    PrintWriter output = null;

    // The handler for socket input.
    // It runs in a separate Thread
    // and does callbacks to this object
    // to inform about what is happening 
    // on the input.
    InputHandler inputHandler = null;

    // The last response from the router
    MCRPResponse lastResponse = null;

    // An exception thrown when trying to get data from the server
    Exception serverException = null;

    // This is the finite state machine
    FSMState fsm = FSMState.FSM_WAITING;


    /* The event listeners. */
    ArrayList <MCRPEventListener> eventListeners = new ArrayList <MCRPEventListener>();

    // debug
    public static boolean debug = false;
	

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public MCRPInteractor(String addr, int port) throws UnknownHostException, IOException  {
	initialize(InetAddress.getByName(addr), port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public MCRPInteractor(InetAddress addr, int port) throws UnknownHostException, IOException  {
	initialize(addr, port);
    }


    /* Initialize the connection */

    /**
     * Initialize a connection to the ManagementConsole of a router.
     * @param addr the address of the server
     * @param port the port the server is listening on
     */
    protected void initialize(InetAddress addr, int port) throws UnknownHostException, IOException  {
	// set the fsm to FSM_READY
	fsm = FSMState.FSM_READY;

	// open the socket
	socket = new Socket(addr, port); 

	// get the input
	input = new InputStreamReader(socket.getInputStream());
	// get the output
	output = new PrintWriter(socket.getOutputStream(), true);

	// create a handler for the input
	inputHandler = new InputHandler(this, input);
    }

    /* Calls for ManagementConsole */

    /**
     * Quit talking to the router
     * Close a connection to the ManagementConsole of the router.
     */
    public MCRPInteractor quit() throws IOException, MCRPException {
	interact(MCRP.QUIT.CMD);
	expect(MCRP.QUIT.CODE);
	return this;
    }

    /**
     * Get the name of the router.
     */
    public String getName() throws IOException, MCRPException {
	MCRPResponse response = interact(MCRP.GET_NAME.CMD);
	expect(MCRP.GET_NAME.CODE);

	return response.get(0)[1];
    }


    /**
     * Set the name of the router.
     * @param name the new name of the router
     */
    public MCRPInteractor setName(String name) throws IOException, MCRPException {
        String toSend = MCRP.SET_NAME.CMD + " " + name;
	interact(toSend);
	expect(MCRP.SET_NAME.CODE);
	return this;
    }

    /**
     * Get the port number to connect to on the router
     * in order to make a new router-to-router data connection.
     */
    public String getConnectionPort() throws IOException, MCRPException {
	MCRPResponse response = interact(MCRP.GET_CONNECTION_PORT.CMD);
	expect(MCRP.GET_CONNECTION_PORT.CODE);

	return response.get(0)[1];
    }


    /**
     * Get the address of a port on the router.
     * Need to specify the address type and address value.
     * @param port the port name
     */
    public String getAddress(String port) throws IOException, MCRPException {
        String toSend = MCRP.GET_ADDRESS.CMD + " " + port ; 
	MCRPResponse response = interact(toSend);
	expect(MCRP.GET_ADDRESS.CODE);

        return response.get(0)[1];
    }

    /**
     * Set the address of a port on the router.
     * Need to specify the address type and address value.
     * @param port the port name
     * @param type the type of the address
     * @param addr the value for the address
     */
    public MCRPInteractor setAddress(String port, String type, String addr) throws IOException, MCRPException {
        String toSend = MCRP.SET_ADDRESS.CMD + " " + port + " " + type + " " + addr; 
	interact(toSend);
	expect(MCRP.SET_ADDRESS.CODE);
	return this;
    }


    /**
     * Tell the router there has been an incoming connection
     * on the router-to-router port.
     * @param connectionID the name for the incoming connection
     * @param name the name of the router making the connection
     * @param weight the weight of the connection
     * @param port the port number
     */
    public MCRPInteractor incomingConnection(String connectionID, String name, int weight, int port) throws IOException, MCRPException {
        String toSend = MCRP.INCOMING_CONNECTION.CMD + " " + connectionID + " " + name + " " + weight  + " " + port ; 
	interact(toSend);
	expect(MCRP.INCOMING_CONNECTION.CODE);
	return this;
    }

    /**
     * List all the router-to-router connections that the router has.
     */
    public  List<String> listConnections()  throws IOException, MCRPException {
	MCRPResponse response = interact(MCRP.LIST_CONNECTIONS.CMD);
	expect(MCRP.LIST_CONNECTIONS.CODE);

        // now we convert the replies in the response
	// into a list of connections

	// get no of connections
	int connectionReplies = response.getReplies() - 1;

	// System.err.println("listConnections: " + connectionReplies + " replies");

	// create a list for the names
	List<String> connectionNames = new ArrayList<String>();

	for (int r=0; r < connectionReplies; r++) {
	    // pick out the r-th connection
	    connectionNames.add(response.get(r)[1]);
	}

	return connectionNames;
    }


    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a 
     * connection weight of connection_weight
     */
    public MCRPInteractor createConnection(String address, int weight) throws IOException, MCRPException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address + " " + weight; 
	interact(toSend);
	expect(MCRP.CREATE_CONNECTION.CODE);
	return this;
    }



    /* Methods for managing event listeners. */

    /**
     * Add an event listener.
     * @param l an object that implements MCRPEventListener
     */
    public void addMCRPEventListener(MCRPEventListener l) {
	eventListeners.add(l);
    }
      
    /**
     * Remove an event listener.
     * @param l an object that implements MCRPEventListener
     */
    public void removeMCRPEventListener(MCRPEventListener l) {
	eventListeners.remove(l);
    }
      

    /**
     * Get the number of listeners.
     */
    public int getMCRPEventListenerCount() {
	return eventListeners.size();
    }

    /**
     * Get the actual listener objects.
     */
    public MCRPEventListener[] getMCRPEventListeners() {
	return (MCRPEventListener[])eventListeners.toArray(new MCRPEventListener[0]);
    }

    /**
     * Pass the event to the all of listeners.
     */
    private void generateEvent(MCRPEvent evt) {
	for (MCRPEventListener l : getMCRPEventListeners()) {
	    l.ssipEvent(evt);
	}
    }

    /*
     * Internal methods.
     */

    /**
     * The callback for the SocketInputHandler, with a response.
     * @param response the response from the server
     */
    protected synchronized void gotResponse(MCRPResponse response) {
	lastResponse = response;

	if (debug) {
	    System.err.println("MCRPInteractor: response code = " + lastResponse.getCode());
	}

	fsm = FSMState.FSM_READY;

	notifyAll();
    }
     
    /**
     * The callback for the SocketInputHandler, with an event.
     * The response is converted into an event object.
     * @param response the response from the server
     * @throws IllegalArgumentException if a response is passed in that does not have an expected event code.
     */
    protected void gotEvent(MCRPResponse response) throws IllegalArgumentException {
	String code = response.getCode();

        /*
	String speechID = response.get(0)[1];
	String clientID = response.get(1)[1];
	MCRPEvent event;

	if (code.equals("700")) {
	    // INDEX_MARK event
	    event = new MCRPEvent(this, MCRPEvent.MCRPEventType.INDEX_MARK, new ID(speechID), new ID(clientID));

	} else if (code.equals("701")) {
	    // BEGIN event
	    event = new MCRPEvent(this, MCRPEvent.MCRPEventType.BEGIN, new ID(speechID), new ID(clientID));

	} else if (code.equals("702")) {
	    // END event
	    event = new MCRPEvent(this, MCRPEvent.MCRPEventType.END, new ID(speechID), new ID(clientID));

	} else if (code.equals("703")) {
	    // CANCEL event
	    event = new MCRPEvent(this, MCRPEvent.MCRPEventType.CANCEL, new ID(speechID), new ID(clientID));

	} else if (code.equals("704")) {
	    // PAUSE event
	    event = new MCRPEvent(this, MCRPEvent.MCRPEventType.PAUSE, new ID(speechID), new ID(clientID));

	} else if (code.equals("705")) {
	    // RESUME event
	    event = new MCRPEvent(this, MCRPEvent.MCRPEventType.RESUME, new ID(speechID), new ID(clientID));

	} else {
	    // not a valid event code
	    throw new IllegalArgumentException("MCRPInteractor: unknown event code from server: " + code);
	}

	//System.err.println("Event = " + event);

	// pass the event to the listeners, if there are any
	if (getMCRPEventListenerCount() > 0) {
	    // pass the event
	    generateEvent(event);
	}
        */

    }
     
    /**
     * The callback for the InputHandler saying EOF.
     */
    protected synchronized void gotEOF() {
	// we got an EOF notification from the InputHandler
	// set the fsm to the end state.
	fsm = FSMState.FSM_END;

	if (debug) {
	    System.err.println("Got EOF");
	}

	// shut the socket and cleanup.
	try {
	    socket.close();
	} catch (IOException ioe) {
	    ;
	} finally {
	    // free up SocketInputHandler
	    inputHandler = null;
	    input = null;
	    output = null;
	    socket = null;
	}
    }
     
    /**
     * The callback from the InputHandler saying there 
     * has been an exception.
     */
    protected synchronized void gotException(Exception e) {
	// we got an Exception notification from the SocketInputHandler
	// set the fsm to the end state.
	fsm = FSMState.FSM_END;

	// save the exception to throw a bit later
	serverException = e;

	// shut the socket and cleanup.
	try {
	    socket.close();
	} catch (IOException ioe) {
	    ;
	} finally {
	    // free up SocketInputHandler
	    inputHandler = null;
	    input = null;
	    output = null;
	    socket = null;


	    // now notifyAll to get out of the wait
	    notifyAll();
	}
    }
     
    /**
     * Interact with the server.
     * This sends a string and waits for a response.
     * @param str The string to pass to the server as the request
     * @return the response
     */
    private synchronized MCRPResponse interact(String str) throws MCRPException {

	// check if we are ready to interact with the server
	if (fsm == FSMState.FSM_READY) {
	    // yes, we are ready
	    // so send the string
	    send(str);

	    // set the finite state machine to state: waiting
	    fsm = FSMState.FSM_WAITING;

	    // sit and wait for a response
	    while (fsm == FSMState.FSM_WAITING) {
		try {
		    wait();
		} catch (InterruptedException ie) {
		    //System.err.println("Got InterruptedException " + ie);
		}
	    }

	    // we got here because there was a notifyAll.
	    // this happens after gotResponse()
	    // or gotException()

	    if (fsm == FSMState.FSM_READY) {
		// that's good
		return lastResponse;

	    } else if (fsm == FSMState.FSM_END) {
		// the fsm was set to the end state.
		// probably EOF
		return lastResponse;
	    } else {
		// an unknown state
		throw new MCRPException("MCRPInteractor: finite state machine in unknown state: " + fsm);
	    }

	} else {
	    // we're not ready to interact with the server
	    if (fsm == FSMState.FSM_WAITING) {
		// we're waiting for a response
		throw new MCRPNotReadyException("MCRPInteractor: not ready to interact with the server.  Awaiting a response from " + str);

	    } else if (fsm == FSMState.FSM_END) {
		// we're at eof
		throw new MCRPNoConnectionException("MCRPInteractor: no connection to server");

	    } else {
		// an unknown state
		throw new MCRPException("MCRPInteractor: finite state machine in unknown state: " + fsm);
	    }
	}
    }

    /**
     * Send the string to the socket
     * @param str The string to pass to the server as the request
     */
    private void send(String str) {
	if (debug) {
	    System.err.print(">> " + str);
	    System.err.print(" ");
	}

	output.print(str);
	output.print("\n");
	output.flush();
    }

    /**
     * Check the code in the last response
     * is the one we expect.
     * @param expected the response code we expect.
     */
    private void expect(int expected) throws MCRPException {
        expect(Integer.toString(expected));
    }

    /**
     * Check the code in the last response
     * is the one we expect.
     * @param expected the response code we expect.
     */
    private void expect(String expected) throws MCRPException {
	String actual = lastResponse.getCode();
	if (expected.equals(actual)) {
	    // everything is OK, the actual code and
	    // the expected code are the same
	    return;
	} else {
	    String message = lastResponse.get(lastResponse.getReplies() - 1)[1];
	    // we got a different code from the expected one
	    throw new MCRPException("Expected return code: " + expected + 
				    " Got: " + actual +
				    " Message is: " + message);
	}

    }


    /**
     * The states of the FSM
     */
    enum FSMState {
        FSM_WAITING,   // waiting for a response
        FSM_READY,     // ready to accept a request
        FSM_END        // we have reached the end state, the connection is closed
    }

}
