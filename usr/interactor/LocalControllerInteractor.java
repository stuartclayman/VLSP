package usr.interactor;

import usr.protocol.MCRP;
import usr.logging.*;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.io.*;
import usr.common.LocalHostInfo;
import java.util.List;
import java.util.ArrayList;
import us.monoid.web.*;
import us.monoid.json.*;

/**
 * This class implements the MCRP protocol and acts as a client
 * for interacting with the ManagementConsole of a LocalController.
 */
public class LocalControllerInteractor {
    // A URI for a LocalController to interact with
    String localControllerURI;
    Resty rest;
    int port;

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a LocalController.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public LocalControllerInteractor(String addr, int port) throws UnknownHostException, IOException  {
        initialize(InetAddress.getByName(addr), port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a LocalController.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public LocalControllerInteractor(InetAddress addr, int port) throws UnknownHostException, IOException  {
        initialize(addr, port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a LocalController.
     * @param lh the LocalHostInfo description
     */
    public LocalControllerInteractor(LocalHostInfo lh) throws UnknownHostException, IOException  {
        initialize(lh.getIp(), lh.getPort());
    }

    /**
     * Initialize
     */
    private synchronized void initialize(InetAddress addr, int port) {
        this.port = port;
        //URI uri = new URI("http", null, addr.toString(), port, null, null, null);
        localControllerURI = "http://" + addr.getHostName() + ":" + Integer.toString(port);

        //Logger.getLogger("log").logln(USR.STDOUT, "localControllerURI: " + localControllerURI);
        
        rest = new Resty();
    }

    /**
     * Get the port this LocalControllerInteractor is connecting to
     */
    public int getPort() {
        return port;
    }

    /**
     * Interact 
     */
    private JSONObject interact(String str) throws IOException, JSONException {
        String uri = localControllerURI +  "/command/" + java.net.URLEncoder.encode(str, "UTF-8");

        Logger.getLogger("log").logln(USR.STDOUT, "LC call: " +  uri.substring(0,Math.min(84, uri.length())));

        JSONObject jsobj = rest.json(uri).toObject();

        Logger.getLogger("log").logln(USR.STDOUT, "LC response: " + jsobj.toString());

        return jsobj;
    }




    /* Calls for ManagementConsole */


    /**
     * Shutdown the LocalController we are interacting with.
     */
    public Boolean shutDown() throws IOException, JSONException {
        JSONObject response = interact(MCRP.SHUT_DOWN.CMD);
        return (Boolean)response.get("success");
    }

    /**
     * As the LocalController to start a new router.
     */
    public JSONObject newRouter(int routerId, int port) throws IOException, JSONException  {
        return newRouter(routerId,port,port+1, null, null);
    }

    /**
     * Ask the LocalController to start a new router.
     * Name is optional.
     */
    public JSONObject newRouter(int routerId, int port1, int port2, String address, String name)
    throws IOException, JSONException  {
        String toSend = MCRP.NEW_ROUTER.CMD+" "+routerId+ " " + port1 + " " + port2;

        // check for address
        if (address != null) {
            toSend += " " + address;

            // and name
            if (name != null) {
                toSend += " " + name;
            }

        }

        JSONObject response = interact(toSend);

        // return the router name
        //return (String)response.get("name");

        return response;
    }

    /**
     * Ask the Local Controller to connect routers
     * Name is optional.
     */
    public String connectRouters(String host1, int port1, String host2, int port2, int weight, String name) throws IOException, JSONException {
        String toSend = MCRP.CONNECT_ROUTERS.CMD+" "+host1+":"+port1+" "+
                        host2+":"+port2 +" " + weight;


        if (name != null) {
            toSend += " " + name;
        }

        JSONObject response = interact(toSend);

        // return the connection name
        return (String)response.get("name");
    }

    /** Ask the Local Controller to stop a router */
    public Boolean endRouter(String host1, int port1) throws IOException, JSONException
    {
        String toSend= MCRP.ROUTER_SHUT_DOWN.CMD + " "+host1+":"+port1;
        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }

    /** Ask the Local Controller to end a link */
    public Boolean endLink(String host1, int port1, String address) throws IOException, JSONException {
        String toSend = MCRP.END_LINK.CMD+" "+host1+":"+port1+" "+address;
        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }

    /** Set the configuration string for a router */
    public Boolean setConfigString(String config) throws IOException,
    JSONException {
        String toSend = MCRP.ROUTER_CONFIG.CMD + " "+ java.net.URLEncoder.encode(config, "UTF-8");
        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }

    /**
     * Ask the LocalController to start a command on a router.
     */
    public JSONObject appStart(int routerId, String className, String[] args) throws IOException, JSONException  {

        StringBuilder builder = new StringBuilder();
        builder.append(MCRP.ON_ROUTER.CMD);
        builder.append(" ");
        builder.append(routerId);
        builder.append(" ");
        builder.append(className);

        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }

        String toSend = builder.toString();

        JSONObject response = interact(toSend);

        // return the response
        return response;
    }


    /**
     * Check with a local controller.
     */
    public Boolean checkLocalController(String host, int port) throws IOException, JSONException {
        String toSend = MCRP.CHECK_LOCAL_CONTROLLER.CMD +
                        " " + host + " " + port;
        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }

    /**
     * Check with a local controller.
     */
    public Boolean checkLocalController(LocalHostInfo gc) throws IOException, JSONException {
        String host= gc.getName();
        int port= gc.getPort();
        String toSend = MCRP.CHECK_LOCAL_CONTROLLER.CMD + " " + host + " " + port;
        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }

    /** Send a message to a local controller intended for a router to
       set its aggregation point */
    public Boolean setAP(int GID, int APGID) throws IOException, JSONException {
        String toSend;
        toSend = MCRP.SET_AP.CMD + " " + GID + " " + APGID;

        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }

    /** Send a message to a local controller informing it about a routers
       status as an aggregation point */
    public Boolean reportAP(int GID, int AP) throws IOException, JSONException {
        String toSend = MCRP.REPORT_AP.CMD + " " + GID + " " +AP;

        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }


    /** Request stats from routers -- stats are returned by a separate command*/
    public void  requestRouterStats() throws IOException, JSONException {

        interact(MCRP.REQUEST_ROUTER_STATS.CMD);
    }

    /**
     * Get the stats from a Router.
     */
    public List<String> getRouterStats() throws IOException, JSONException {
        // 285-1 localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0 OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 285-1 /Router-15151-15152/Connection-1  InBytes=66 InPackets=1 InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 285 END 2

        JSONObject response = interact(MCRP.GET_ROUTER_STATS.CMD);

        // now we convert the replies in the response
        // into a list of connections

        // get no of netifs
        Integer routerReplies = (Integer)response.get("size");

        // create a list for the names
        List<String> stats = new ArrayList<String>();

        for (int r=0; r < routerReplies; r++) {
            // pick out the r-th connection
            stats.add((String)response.get(Integer.toString(r)));
        }

        return stats;
    }

    /**
     * Monitoring Start.
     * @param addr The InetSocketAddress of the Monitoring data consumer
     * @param howOften How many seconds between measurements
     */
    public Boolean monitoringStart(InetSocketAddress addr, int howOften) throws IOException, JSONException {
        String toSend = MCRP.MONITORING_START.CMD + " " + addr.getAddress().getHostAddress() + ":" + addr.getPort() + " " + howOften;
        JSONObject response = interact(toSend);
        return (Boolean)response.get("success");
    }

    /**
     * Monitoring Stop
     */
    public Boolean monitoringStop() throws IOException, JSONException {
        JSONObject response = interact(MCRP.MONITORING_STOP.CMD);
        return (Boolean)response.get("success");
    }



}
