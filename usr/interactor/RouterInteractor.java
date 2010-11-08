package usr.interactor;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.net.Address;
import usr.net.GIDAddress;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This class implements the MCRP protocol and acts as a client
 * for interacting with the ManagementConsole of a Router.
 */
public class RouterInteractor extends MCRPInteractor {

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

    /* Calls for Router ManagementConsole */

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
     * Get the global address of the router.
     */
    public Address getRouterAddress() throws IOException, MCRPException {
	MCRPResponse response = interact(MCRP.GET_ROUTER_ADDRESS.CMD);
	expect(MCRP.GET_ROUTER_ADDRESS.CODE);

        String value = response.get(0)[1];

        Address addr = new GIDAddress(value);

        return addr;
        /*
        Scanner scanner = new Scanner(value);

        if (scanner.hasNextInt()) {
            int id = scanner.nextInt();

            return id;
        } else {
            return -1;
        }
        */
    }


    /**
     * Set the global address of the router.
     * @param addr the address of the router
     */
    public MCRPInteractor setRouterAddress(Address addr) throws IOException, MCRPException {
        int id = addr.asInteger();
        String toSend = MCRP.SET_ROUTER_ADDRESS.CMD + " " + id;
	interact(toSend);
	expect(MCRP.SET_ROUTER_ADDRESS.CODE);
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
     * Get the name of a port on the router.
     * @param port the port name
     */
    public String getPortName(String port) throws IOException, MCRPException {
        String toSend = MCRP.GET_PORT_NAME.CMD + " " + port ; 
	MCRPResponse response = interact(toSend);
	expect(MCRP.GET_PORT_NAME.CODE);

        return response.get(0)[1];
    }

    /**
     * Get the name of a remote router of a port on the router.
     * @param port the port name
     */
    public String getPortRemoteRouterName(String port) throws IOException, MCRPException {
        String toSend = MCRP.GET_PORT_REMOTE_ROUTER.CMD + " " + port ; 
	MCRPResponse response = interact(toSend);
	expect(MCRP.GET_PORT_REMOTE_ROUTER.CODE);

        return response.get(0)[1];
    }

    /**
     * Get the address of a remote router of a port on the router.
     * @param port the port name
     */
    public String getPortRemoteAddress(String port) throws IOException, MCRPException {
        String toSend = MCRP.GET_PORT_REMOTE_ADDRESS.CMD + " " + port ; 
	MCRPResponse response = interact(toSend);
	expect(MCRP.GET_PORT_REMOTE_ADDRESS.CODE);

        return response.get(0)[1];
    }

    /**
     * Get the address of a port on the router.
     * @param port the port name
     */
    public String getPortAddress(String port) throws IOException, MCRPException {
        String toSend = MCRP.GET_PORT_ADDRESS.CMD + " " + port ; 
	MCRPResponse response = interact(toSend);
	expect(MCRP.GET_PORT_ADDRESS.CODE);

        return response.get(0)[1];
    }

    /**
     * Set the address of a port on the router.
     * Need to specify the address type and address value.
     * @param port the port name
     * @param type the type of the address
     * @param addr the value for the address
     */
    public MCRPInteractor setPortAddress(String port, String type, String addr) throws IOException, MCRPException {
        String toSend = MCRP.SET_PORT_ADDRESS.CMD + " " + port + " " + type + " " + addr; 
	interact(toSend);
	expect(MCRP.SET_PORT_ADDRESS.CODE);
	return this;
    }

    /**
     * Get the weight of a port on the router.
     * @param port the port name
     */
    public String getPortWeight(String port) throws IOException, MCRPException {
        String toSend = MCRP.GET_PORT_WEIGHT.CMD + " " + port ; 
	MCRPResponse response = interact(toSend);
	expect(MCRP.GET_PORT_WEIGHT.CODE);

        return response.get(0)[1];
    }

    /**
     * Set the weight of a port on the router.
     * Need to specify the weight type and weight value.
     * @param port the port name
     * @param weight the value for the weight
     */
    public MCRPInteractor setPortWeight(String port, String weight) throws IOException, MCRPException {
        String toSend = MCRP.SET_PORT_WEIGHT.CMD + " " + port + " " + weight; 
	interact(toSend);
	expect(MCRP.SET_PORT_WEIGHT.CODE);
	return this;
    }


    /**
     * Tell the router there has been an incoming connection
     * on the router-to-router port.
     * @param connectionID the name for the incoming connection
     * @param name the name of the router making the connection
     * @param addr the address of the router making the connection
     * @param weight the weight of the connection
     * @param port the port number
     */
    public MCRPInteractor incomingConnection(String connectionID, String name, Address addr, int weight, int port) throws IOException, MCRPException {
        String toSend = MCRP.INCOMING_CONNECTION.CMD + " " + connectionID + " " + name + " " + addr.asInteger() + " " + weight  + " " + port ; 
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

	// Logger.getLogger("log").logln(USR.ERROR, "listConnections: " + connectionReplies + " replies");

	// create a list for the names
	List<String> connectionNames = new ArrayList<String>();

	for (int r=0; r < connectionReplies; r++) {
	    // pick out the r-th connection
	    connectionNames.add(response.get(r)[1]);
	}

	return connectionNames;
    }

    /**
     * Start an app.
     * APP_START classname args
     */
    public String appStart(String className, String[] args)  throws IOException, MCRPException {
        StringBuilder builder = new StringBuilder();
        builder.append(MCRP.APP_START.CMD);
        builder.append(" ");
        builder.append(className);

        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }

        String toSend = builder.toString();

        MCRPResponse response = interact(toSend);
	expect(MCRP.APP_START.CODE);

        return response.get(0)[1];
    }

    /**
     * Stop an app
     * APP_STOP app_name
     */
    public String appStop(String appName)  throws IOException, MCRPException {
        MCRPResponse response = interact(MCRP.APP_STOP.CMD);
	expect(MCRP.APP_STOP.CODE);

	return response.get(0)[1];
    }


    /**
     * List all app
     * APP_LIST
     */
    public List<String> appList()  throws IOException, MCRPException {
        MCRPResponse response = interact(MCRP.APP_LIST.CMD);
	expect(MCRP.APP_LIST.CODE);

        // now we convert the replies in the response
	// into a list of apps

	// get no of apps
	int appReplies = response.getReplies() - 1;

	// Logger.getLogger("log").logln(USR.ERROR, "appList: " + appReplies + " replies");

	// create a list for the names
	List<String> appNames = new ArrayList<String>();

	for (int r=0; r < appReplies; r++) {
	    // pick out the r-th app
	    appNames.add(response.get(r)[1]);
	}

	return appNames;

    }


    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a 
     * connection weight of connection_weight
     * @return the name of the created connection, e.g. /Router-28/Connection-1
     */
    public String createConnection(String address, int weight) throws IOException, MCRPException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address + " " + weight; 
	MCRPResponse response = interact(toSend);
	expect(MCRP.CREATE_CONNECTION.CODE);

        // return the connection name
	return response.get(0)[1];
    }
    
      /** End the link from this router to the router with a given id */
    public MCRPInteractor endLink(String rId) throws IOException, 
        MCRPException {
          String toSend = MCRP.END_LINK.CMD + " "+ rId;
          interact(toSend);
          expect(MCRP.END_LINK.CODE);
          return this;

    }
    
    /** Set the configuration string for a router */
    public MCRPInteractor setConfigString(String config) throws IOException, 
        MCRPException {
          String toSend = MCRP.READ_OPTIONS_STRING.CMD + " "+ config;
          interact(toSend);
          expect(MCRP.READ_OPTIONS_STRING.CODE);
          return this;

    }
    
    /** Send a message to a local controller intended for a router to 
      set its status as an aggregation point */
    public MCRPInteractor setAP(int GID, int APGID) throws IOException, MCRPException {
        String toSend;
        toSend = MCRP.SET_AP.CMD + 
            " " + GID + " " + APGID;
       
	      interact(toSend);
	      expect(MCRP.SET_AP.CODE);
	      return this;
    }
    
    /**
     * Get the NetIF stats from a Router.
     */
    public List<String> getNetIFStats() throws IOException, MCRPException {
        // 237-localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0 OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 237-/Router-15151-15152/Connection-1  InBytes=66 InPackets=1 InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 237 END 2

	MCRPResponse response = interact(MCRP.GET_NETIF_STATS.CMD);
	expect(MCRP.GET_NETIF_STATS.CODE);

        // now we convert the replies in the response
	// into a list of connections

        // get no of netifs
	int netifReplies = response.getReplies() - 1;

	// create a list for the names
	List<String> stats = new ArrayList<String>();

	for (int n=0; n < netifReplies; n++) {
	    // pick out the r-th connection
	    stats.add(response.get(n)[1]);
	}

	return stats;
    }


    /**
     * Get the socket stats from a Router.
     */
    public List<String> getSocketStats() throws IOException, MCRPException{
        // 237-localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0 OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 237-/Router-15151-15152/Connection-1  InBytes=66 InPackets=1 InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 237 END 2

	MCRPResponse response = interact(MCRP.GET_SOCKET_STATS.CMD);
	expect(MCRP.GET_SOCKET_STATS.CODE);

        // now we convert the replies in the response
	// into a list of connections

        // get no of sockets
	int socketReplies = response.getReplies() - 1;

	// create a list for the names
	List<String> stats = new ArrayList<String>();

	for (int s=0; s < socketReplies; s++) {
	    // pick out the s-th connection
	    stats.add(response.get(s)[1]);
	}

	return stats;
    }


    /**
     * Shutdown the Router we are interacting with.
     */
    public MCRPInteractor shutDown() throws IOException, MCRPException {
         
        interact(MCRP.SHUT_DOWN.CMD);
        expect(MCRP.SHUT_DOWN.CODE);
        return this;
    }


}
