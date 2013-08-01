package usr.interactor;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.net.Address;
import usr.net.AddressFactory;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import us.monoid.web.*;
import us.monoid.json.*;

/**
 * This class implements the REST protocol and acts as a client
 * for interacting with the ManagementConsole of a Router.
 */
public class RouterInteractor {
    // A URI for a router to interact with
    String routerURI;
    Resty rest;
    int port;

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public RouterInteractor(String addr, int port) throws UnknownHostException,
    IOException {
        initialize(InetAddress.getByName(addr), port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public RouterInteractor(InetAddress addr, int port) throws UnknownHostException,
    IOException {
        initialize(addr, port);
    }

    /**
     * Initialize
     */
    private synchronized void initialize(InetAddress addr, int port) {
        this.port = port;

        //URI uri = new URI("http", null, addr.toString(), port, null,
        // null,
        // null);
        routerURI = "http://" + addr.getHostName() + ":"
            + Integer.toString(port);
        Logger.getLogger("log").logln(USR.STDOUT,
                                      "routerURI: " + routerURI);
        rest = new Resty();
    }

    /**
     * Get the port this RouterInteractor is connecting to
     */
    public int getPort() {
        return port;
    }

    /**
     * Interact
     */
    private JSONObject interact(String str) throws IOException,
    JSONException {
        String uri = routerURI + "/command/"
            + java.net.URLEncoder.encode(str, "UTF-8");

        Logger.getLogger("log").logln(USR.STDOUT, "call: "
                                      + uri.substring(0, Math.min(64, uri.length())));
        JSONObject jsobj = rest.json(uri).toObject();
        Logger.getLogger("log").logln(USR.STDOUT,
                                      "response: " + jsobj.toString());
        return jsobj;
    }

    /* Calls for Router ManagementConsole */

    /**
     * Quit talking to the router
     * Close a connection to the ManagementConsole of the router.
     */
    public RouterInteractor quit() throws IOException, JSONException {
        //interact(MCRP.QUIT.CMD);
        return this;
    }

    /**
     * Get the name of the router.
     */
    public String getName() throws IOException, JSONException {
        JSONObject response = interact(MCRP.GET_NAME.CMD);

        return (String)response.get("name");
    }

    /**
     * Set the name of the router.
     * @param name the new name of the router
     */
    public RouterInteractor setName(String name) throws IOException,
    JSONException {
        String toSend = MCRP.SET_NAME.CMD + " " + name;

        interact(toSend);
        return this;
    }

    /**
     * Get the address of the router.
     */
    public Address getRouterAddress() throws IOException,
    JSONException {
        JSONObject response = interact(MCRP.GET_ROUTER_ADDRESS.CMD);

        String value = (String)response.get("address");

        Address addr = AddressFactory.newAddress(value);

        return addr;
    }

    /**
     * Set the global address of the router.
     * @param addr the address of the router
     */
    public RouterInteractor setRouterAddress(Address addr) throws
    IOException, JSONException {
        String id = addr.asTransmitForm();
        String toSend = MCRP.SET_ROUTER_ADDRESS.CMD + " " + id;

        interact(toSend);

        return this;
    }

    /**
     * Get the port number to connect to on the router
     * in order to make a new router-to-router data connection.
     */
    public String getConnectionPort() throws IOException,
    JSONException {
        JSONObject response = interact(MCRP.GET_CONNECTION_PORT.CMD);

        return (String)response.get("port");
    }

    /**
     * Get the name of a port on the router.
     * @param port the port name
     */
    public String getPortName(String port) throws IOException,
    JSONException {
        String toSend = MCRP.GET_PORT_NAME.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("name");
    }

    /**
     * Get the name of a remote router of a port on the router.
     * @param port the port name
     */
    public String getPortRemoteRouterName(String port) throws
    IOException,
    JSONException {
        String toSend = MCRP.GET_PORT_REMOTE_ROUTER.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("name");
    }

    /**
     * Get the address of a remote router of a port on the router.
     * @param port the port name
     */
    public String getPortRemoteAddress(String port) throws IOException,
    JSONException {
        String toSend = MCRP.GET_PORT_REMOTE_ADDRESS.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("address");
    }

    /**
     * Get the address of a port on the router.
     * @param port the port name
     */
    public String getPortAddress(String port) throws IOException,
    JSONException {
        String toSend = MCRP.GET_PORT_ADDRESS.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("address");
    }

    /**
     * Set the address of a port on the router.
     * Need to specify the address type and address value.
     * @param port the port name
     * @param addr the value for the address
     */
    public RouterInteractor setPortAddress(String port, String type, String addr) throws
    IOException,
    JSONException {
        String toSend = MCRP.SET_PORT_ADDRESS.CMD + " " + port + " "
            + addr;

        interact(toSend);
        return this;
    }

    /**
     * Get the weight of a port on the router.
     * @param port the port name
     */
    public String getPortWeight(String port) throws IOException,
    JSONException {
        String toSend = MCRP.GET_PORT_WEIGHT.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("weight");
    }

    /**
     * Set the weight of a port on the router.
     * Need to specify the weight type and weight value.
     * @param port the port name
     * @param weight the value for the weight
     */
    public RouterInteractor setPortWeight(String port, String weight) throws
    IOException,
    JSONException {
        String toSend = MCRP.SET_PORT_WEIGHT.CMD + " " + port + " "
            + weight;

        interact(toSend);
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
    public RouterInteractor incomingConnection(String connectionID, String name, Address addr, int weight, int port) throws
    IOException,
    JSONException {
        String toSend = MCRP.INCOMING_CONNECTION.CMD + " "
            + connectionID
            + " " + name + " " + addr.asTransmitForm()
            + " "
            + weight + " "
            + port;

        interact(toSend);
        return this;
    }

    /**
     * List all the router-to-router connections that the router has.
     */
    public List<String> listConnections()  throws IOException,
    JSONException {
        JSONObject response = interact(MCRP.LIST_CONNECTIONS.CMD);

        // now we convert the replies in the response
        // into a list of connections

        // get no of connections
        Integer connectionReplies = (Integer)response.get("size");

        // Logger.getLogger("log").logln(USR.ERROR, "listConnections: "
        // +
        // connectionReplies + " replies");

        // create a list for the names
        List<String> connectionNames = new ArrayList<String>();

        for (int r = 0; r < connectionReplies; r++) {
            // pick out the r-th connection
            connectionNames.add((String)response.get(Integer.toString(r)));
        }

        return connectionNames;
    }

    /**
     * Start an app.
     * APP_START classname args
     */
    public JSONObject appStart(String className, String[] args)  throws IOException,
    JSONException {
        StringBuilder builder = new StringBuilder();

        builder.append(MCRP.APP_START.CMD);
        builder.append(" ");
        builder.append(className);

        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }

        String toSend = builder.toString();

        JSONObject response = interact(toSend);

        return response;
    }

    /**
     * Stop an app
     * APP_STOP app_name
     */
    public String appStop(String appName)  throws IOException,
    JSONException {
        JSONObject response = interact(MCRP.APP_STOP.CMD);

        return (String)response.get("response");
    }

    /**
     * List all app
     * APP_LIST
     */
    public List<String> appList()  throws IOException, JSONException {
        JSONObject response = interact(MCRP.APP_LIST.CMD);

        // now we convert the replies in the response
        // into a list of apps

        // get no of apps
        String appReplies = (String)response.get("size");

        // Logger.getLogger("log").logln(USR.ERROR, "appList: " +
        // appReplies
        // + " replies");

        // create a list for the names
        List<String> appNames = new ArrayList<String>();

        /** old way
         * for (int r=0; r < appReplies; r++) {
         *  // pick out the r-th app
         *  appNames.add(response.get(r)[1]);
         * }
         *
         */

        JSONArray jsarr = (JSONArray)response.get("list");

        /// TODO:  RESOLVE ACTUAL METHOD
        // appNames = jsarr.asArray();

        return appNames;
    }

    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new
     *******************************network
     * interface to a router on the address ip_addr/port
     * @return the name of the created connection, e.g.
     * /Router - 28 / Connection - 1
     */

    public String createConnection(String address) throws IOException,
    JSONException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address;
        JSONObject response = interact(toSend);

        // return the connection name
        return (String)response.get("name");
    }

    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new
     *******************************network
     * interface to a router on the address ip_addr/port with a
     * connection weight of connection_weight
     * @return the name of the created connection, e.g.
     * /Router - 28 / Connection - 1
     */
    public String createConnection(String address, int weight) throws IOException,
    JSONException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address
            + " "
            + weight;
        JSONObject response = interact(toSend);

        // return the connection name
        return (String)response.get("name");
    }

    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight connection_name -
     *******************************create a new network
     * interface to a router on the address ip_addr/port with a
     * connection weight of connection_weight and a name of connection_name
     * @return the name of the created connection, i.e. connection_name
     */
    public String createConnection(String address, int weight, String name) throws IOException,
    JSONException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address
            + " "
            + weight + " " + name;
        JSONObject response = interact(toSend);

        // return the connection name
        return (String)response.get("name");
    }

    /** End the link from this router to the router with a given id */
    public RouterInteractor endLink(String rId) throws IOException,
    JSONException {
        String toSend = MCRP.END_LINK.CMD + " " + rId;

        interact(toSend);
        return this;
    }

    /** Set the configuration string for a router */
    public RouterInteractor setConfigString(String config) throws
    IOException,
    JSONException {
        String toSend = MCRP.READ_OPTIONS_STRING.CMD + " "
            + java.net.URLEncoder.encode(config, "UTF-8");

        interact(toSend);
        return this;
    }

    /** Send a message to a local controller intended for a router to
     * set its status as an aggregation point */
    public RouterInteractor setAP(int GID, int APGID) throws IOException,
    JSONException {
        String toSend;

        toSend = MCRP.SET_AP.CMD + " " + GID + " " + APGID;
        interact(toSend);
        return this;
    }

    /**
     * Get the NetIF stats from a Router.
     */
    public List<String> getNetIFStats() throws IOException {
        // 237-localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0
        // OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0
        // InQueue=0
        // OutQueue=0
        // 237-/Router-15151-15152/Connection-1  InBytes=66 InPackets=1
        // InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501
        // OutErrors=0
        // OutDropped=0 InQueue=0 OutQueue=0
        // 237 END 2
        JSONObject response;

        try {
            response = interact(MCRP.GET_NETIF_STATS.CMD);
        } catch (JSONException e) {
            throw new IOException("Response to rest call threw error "
                                  + e.getMessage());
        }

        // now we convert the replies in the response
        // into a list of connections

        // get no of netifs
        Integer netifReplies;
        try {
            netifReplies = (Integer)response.get("size");
        } catch (JSONException e) {
            throw new IOException("JSON response from router invalid");
        }

        // create a list for the names
        List<String> stats = new ArrayList<String>();

        for (int n = 0; n < netifReplies; n++) {
            // pick out the r-th connection
            try {
                stats.add((String)response.get(Integer.toString(n)));
            } catch (JSONException e) {
                throw new IOException("JSON response from router format "
                                      + "error missing response " + n);
            }
        }

        return stats;
    }

    /**
     * Get the socket stats from a Router.
     */
    public List<String> getSocketStats() throws IOException,
    JSONException {
        // 237-localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0
        // OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0
        // InQueue=0
        // OutQueue=0
        // 237-/Router-15151-15152/Connection-1  InBytes=66 InPackets=1
        // InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501
        // OutErrors=0
        // OutDropped=0 InQueue=0 OutQueue=0
        // 237 END 2

        JSONObject response = interact(MCRP.GET_SOCKET_STATS.CMD);

        // now we convert the replies in the response
        // into a list of connections

        // get no of sockets
        Integer socketReplies = (Integer)response.get("size");

        // create a list for the names
        List<String> stats = new ArrayList<String>();

        for (int s = 0; s < socketReplies; s++) {
            // pick out the s-th connection
            stats.add((String)response.get(Integer.toString(s)));
        }

        return stats;
    }

    /**
     * Monitoring Start.
     * @param addr The InetSocketAddress of the Monitoring data consumer
     * @param howOften How many seconds between measurements
     */
    public RouterInteractor monitoringStart(InetSocketAddress addr, int howOften)
    throws
    IOException, JSONException {
        String toSend = MCRP.MONITORING_START.CMD + " "
            + addr.getAddress().getHostAddress() + ":" +
            addr.getPort()
            + " " + howOften;
        JSONObject response = interact(toSend);

        return this;
    }

    /**
     * Monitoring Stop
     */
    public RouterInteractor monitoringStop() throws IOException,
    JSONException {
        interact(MCRP.MONITORING_STOP.CMD);
        return this;
    }

    /** Check router is responding */
    public boolean routerOK() throws IOException, JSONException {
        interact(MCRP.ROUTER_OK.CMD);
        return true;
    }

    /**
     * Shutdown the Router we are interacting with.
     */
    public Boolean shutDown() throws IOException, JSONException {
        JSONObject response = interact(MCRP.SHUT_DOWN.CMD);

        return (Boolean)response.get("success");
    }

}