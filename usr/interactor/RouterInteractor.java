package usr.interactor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import us.monoid.json.*;
import us.monoid.web.*;
import usr.common.ANSI;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.protocol.MCRP;


/**
 * This class implements the REST protocol and acts as a client
 * for interacting with the ManagementConsole of a Router.
 */
public class RouterInteractor {
    // A URI for a router to interact with
    String routerURI;
    Resty rest;
    InetAddress addr;
    int port;

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
     * Initialize
     */
    private synchronized void initialize(InetAddress addr, int port) {
        this.addr = addr;
        this.port = port;
        //URI uri = new URI("http", null, addr.toString(), port, null, null, null);
        routerURI = "http://" + addr.getHostName() + ":" + Integer.toString(port);

        //Logger.getLogger("log").logln(USR.STDOUT, "routerURI: " + routerURI);

        rest = new Resty();
    }

    /**
     * Get the address of the host this RouterInteractor is connecting to
     */
    public InetAddress getInetAddress() {
        return addr;
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
    private JSONObject interact(String str) throws IOException, JSONException {
        String uri = routerURI +  "/command/" + java.net.URLEncoder.encode(str, "UTF-8");

        Logger.getLogger("log").logln(USR.STDOUT, ANSI.YELLOW + "R call: " + uri.substring(0, Math.min(72, uri.length())) + ANSI.RESET_COLOUR);

        JSONObject jsobj = null;

        try {
            jsobj = rest.json(uri).toObject();
            String stri = jsobj.toString();

            Logger.getLogger("log").logln(USR.STDOUT, ANSI.MAGENTA +  "R response: " + stri.substring(0, Math.min(stri.length(), 72)) + ANSI.RESET_COLOUR);

            return jsobj;
        } catch (java.net.ConnectException ce) {
            Logger.getLogger("log").logln(USR.STDOUT, ANSI.RED + "R ConnectException: " + "Router not listening "  + ANSI.RESET_COLOUR);
            throw ce;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.STDOUT, ANSI.RED + "R IOException: " + ioe.getMessage() + ANSI.RESET_COLOUR);
            ioe.printStackTrace();
            throw ioe;
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.STDOUT, ANSI.RED + "R JSONException: " + je.getMessage() + ANSI.RESET_COLOUR);
            Logger.getLogger("log").logln(USR.STDOUT, "Sent: " + uri);
            Logger.getLogger("log").logln(USR.STDOUT, "Recv: " + jsobj);

            //je.printStackTrace();
            throw je;
        }
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
    public RouterInteractor setName(String name) throws IOException, JSONException {
        String toSend = MCRP.SET_NAME.CMD + " " + name;
        interact(toSend);
        return this;
    }

    /**
     * Get the address of the router.
     */
    public Address getAddress() throws IOException, JSONException {
        JSONObject response = interact(MCRP.GET_ROUTER_ADDRESS.CMD);

        String value = (String)response.get("address");

        Address addr = AddressFactory.newAddress(value);

        return addr;
    }

    /**
     * Set the global address of the router.
     * @param addr the address of the router
     */
    public RouterInteractor setAddress(Address addr) throws IOException, JSONException {
        String id = addr.asTransmitForm();
        String toSend = MCRP.SET_ROUTER_ADDRESS.CMD + " " + id;
        interact(toSend);

        return this;
    }

    /**
     * Get the address of the Management Console as hostname:port
     */
    public String getManagementConsoleAddress() throws IOException {
        return addr.getHostName() + ":" + Integer.toString(port);
    }

    /**
     * Get the port number of the Management Console
     */
    public String getManagementConsolePort() throws IOException {

        return Integer.toString(port);
    }

    /**
     * Get the port number to connect to on the router
     * in order to make a new router-to-router data connection.
     */
    public String getConnectionPort() throws IOException, JSONException {
        JSONObject response = interact(MCRP.GET_CONNECTION_PORT.CMD);

        return (String)response.get("port");
    }

    /**
     * Get the name of a port on the router.
     * @param port the port name
     */
    public String getPortName(String port) throws IOException, JSONException {
        String toSend = MCRP.GET_PORT_NAME.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("name");
    }

    /**
     * Get the name of a remote router of a port on the router.
     * @param port the port name
     */
    public String getPortRemoteRouterName(String port) throws IOException, JSONException {
        String toSend = MCRP.GET_PORT_REMOTE_ROUTER.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("name");
    }

    /**
     * Get the address of a remote router of a port on the router.
     * @param port the port name
     */
    public String getPortRemoteAddress(String port) throws IOException, JSONException {
        String toSend = MCRP.GET_PORT_REMOTE_ADDRESS.CMD + " " + port;
        JSONObject response = interact(toSend);

        return (String)response.get("address");
    }

    /**
     * Get the address of a port on the router.
     * @param port the port name
     */
    public String getPortAddress(String port) throws IOException, JSONException {
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
    public RouterInteractor setPortAddress(String port, String type, String addr) throws IOException, JSONException {
        String toSend = MCRP.SET_PORT_ADDRESS.CMD + " " + port + " " + addr;
        interact(toSend);
        return this;
    }

    /**
     * Get the weight of a port on the router.
     * @param port the port name
     */
    public String getPortWeight(String port) throws IOException, JSONException {
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
    public RouterInteractor setPortWeight(String port, String weight) throws IOException, JSONException {
        String toSend = MCRP.SET_PORT_WEIGHT.CMD + " " + port + " " + weight;
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
     * @param hashCode the hashCode for a connection
     * @param endPointHost the host address for this host 
     * @param endPointPort the host port for this host
     */
    public JSONObject incomingConnection(String connectionID, String name, Address addr, int weight, int hash, InetAddress endPointHost, int endPointPort) throws IOException, JSONException {
        String toSend = MCRP.INCOMING_CONNECTION.CMD + " " + connectionID + " " + name + " " + addr.asTransmitForm() + " " + weight  + " " + hash + " " + endPointHost.getHostAddress() + " " + endPointPort;
        JSONObject response = interact(toSend);
        return response;
    }

    /**
     * List all the router-to-router connections that the router has.
     */
    public List<String> listConnections()  throws IOException, JSONException {
        JSONObject response = interact(MCRP.LIST_CONNECTIONS.CMD);

        // now we convert the replies in the response
        // into a list of connections

        // get no of connections
        Integer connectionReplies = (Integer)response.get("size");

        // Logger.getLogger("log").logln(USR.ERROR, "listConnections: " + connectionReplies + " replies");

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
    public JSONObject appStart(String className, String[] args)  throws IOException, JSONException {
        StringBuilder builder = new StringBuilder();
        builder.append(MCRP.APP_START.CMD);
        builder.append(" ");
        builder.append(className);

        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }

        String toSend = java.net.URLEncoder.encode(builder.toString(), "UTF-8");

        JSONObject response = interact(toSend);

        return response;
    }

    /**
     * Stop an app
     * APP_STOP app_name
     */
    public JSONObject appStop(String appName)  throws IOException, JSONException {
        StringBuilder builder = new StringBuilder();
        builder.append(MCRP.APP_STOP.CMD);
        builder.append(" ");
        builder.append(java.net.URLEncoder.encode(appName, "UTF-8"));

        String toSend = builder.toString();

        JSONObject response = interact(toSend);

        return response;
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

        // Logger.getLogger("log").logln(USR.ERROR, "appList: " + appReplies + " replies");

        // create a list for the names
        List<String> appNames = new ArrayList<String>();

        JSONArray jsarr = (JSONArray)response.get("list");

        return appNames;

    }

    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port
     * @return the name of the created connection, e.g. /Router-28/Connection-1
     */
    public JSONObject createConnection(String address) throws IOException, JSONException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address;
        JSONObject response = interact(toSend);

        // return the connection name
        //return (String)response.get("name");

        return response;
    }

    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a
     * connection weight of connection_weight
     * @return the name of the created connection, e.g. /Router-28/Connection-1
     */
    public JSONObject createConnection(String address, int weight) throws IOException, JSONException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address + " " + weight;
        JSONObject response = interact(toSend);

        // return the connection name
        //return (String)response.get("name");

        return response;
    }

    /**
     * Create a new router-to-router data connection to another router.
     * CREATE_CONNECTION ip_addr/port connection_weight connection_name - create a new network
     * interface to a router on the address ip_addr/port with a
     * connection weight of connection_weight and a name of connection_name
     * @return the name of the created connection, i.e. connection_name
     */
    public JSONObject createConnection(String address, int weight, String name) throws IOException, JSONException {
        String toSend = MCRP.CREATE_CONNECTION.CMD + " " + address + " " + weight + " " + name;
        JSONObject response = interact(toSend);

        // return the connection name
        //return (String)response.get("name");

        return response;
    }

    /** End the link from this router to the router with a given spec */
    public JSONObject endLink(String spec) throws IOException, JSONException {
        String toSend = MCRP.END_LINK.CMD + " "+ spec;
        JSONObject response = interact(toSend);
        return response;

    }

    /** Set the link weight from this router to the router with a given spec */
    public JSONObject setLinkWeight(String spec, int weight) throws IOException, JSONException {
        String toSend = MCRP.SET_LINK_WEIGHT.CMD + " "+ spec+" "+weight;
        JSONObject response = interact(toSend);
        return response;

    }

    /** Set the configuration string for a router */
    public RouterInteractor setConfigString(String config) throws IOException, JSONException {
        String toSend = MCRP.READ_OPTIONS_STRING.CMD + " "+ java.net.URLEncoder.encode(config, "UTF-8");
        interact(toSend);
        return this;

    }

    /** Send a message to a local controller intended for a router to
       set its status as an aggregation point */
    public RouterInteractor setAP(int GID, int APGID, String[] ctxArgs) throws IOException, JSONException {
        //String toSend = MCRP.SET_AP.CMD + " " + GID + " " + APGID;

        StringBuilder builder = new StringBuilder();
        builder.append(MCRP.SET_AP.CMD);
        builder.append(" ");
        builder.append(GID);
        builder.append(" ");
        builder.append(APGID);

        for (String arg : ctxArgs) {
            builder.append(" ");
            builder.append(arg);
        }

        String toSend = java.net.URLEncoder.encode(builder.toString(), "UTF-8");

        interact(toSend);
        return this;
    }

    /**
     * Get the NetIF stats from a Router.
     */
    public List<String> getNetIFStats() throws IOException, JSONException {
        // 237-localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0 OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0  InQueue=0 OutQueue=0
        // 237-/Router-15151-15152/Connection-1  InBytes=66 InPackets=1 InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 237 END 2

        JSONObject response = interact(MCRP.GET_NETIF_STATS.CMD);

        // now we convert the replies in the response
        // into a list of connections

        // get no of netifs
        Integer netifReplies = (Integer)response.get("size");


        /* response.remove("size"); */

        // create a list for the names
        List<String> stats = new ArrayList<String>();
        //System.err.println("RouterInteractor: getNetIFStats JSON response "+response+"\n"+routerURI+"\n");

        Iterator<String> itr = response.keys();

        while (itr.hasNext()) {
            String key = (String)itr.next();

            if (key.equals("size")) {
                continue;
            } else {
                stats.add((String)response.getString(key));
            }
        }

        /*
        for (int n = 0; n < netifReplies; n++) {
            // pick out the r-th connection
            stats.add((String)response.get(Integer.toString(n)));
        }
        */


        return stats;
    }

    /**
     * Get the socket stats from a Router.
     */
    public List<String> getSocketStats() throws IOException, JSONException {
        // 237-localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0 OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0
        // InQueue=0 OutQueue=0
        // 237-/Router-15151-15152/Connection-1  InBytes=66 InPackets=1 InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501
        // OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
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
    public RouterInteractor monitoringStart(InetSocketAddress addr, int howOften) throws IOException, JSONException {
        String toSend = MCRP.MONITORING_START.CMD + " " + addr.getAddress().getHostAddress() + ":" + addr.getPort() + " " +
            howOften;
        interact(toSend);
        return this;
    }

    /**
     * Monitoring Stop
     */
    public RouterInteractor monitoringStop() throws IOException, JSONException {
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
