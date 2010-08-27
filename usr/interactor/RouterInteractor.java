package usr.interactor;

import usr.console.MCRP;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;    /* Calls for ManagementConsole */

class RouterInteractor extends MCRPInteractor
{
    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public RouterInteractor(String addr, int port) throws UnknownHostException, IOException  {
	initialize(InetAddress.getByName(addr), port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public RouterInteractor(InetAddress addr, int port) throws UnknownHostException, IOException  {
	initialize(addr, port);
    }

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

}
