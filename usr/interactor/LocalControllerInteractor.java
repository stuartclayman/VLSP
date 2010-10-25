package usr.interactor;

import usr.protocol.MCRP;
import usr.logging.*;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import usr.common.LocalHostInfo;
import java.util.List;
import java.util.ArrayList;

/**
 * This class implements the MCRP protocol and acts as a client
 * for interacting with the ManagementConsole of a LocalController.
 */
public class LocalControllerInteractor extends MCRPInteractor
{
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

   /* Calls for ManagementConsole */


    /**
     * Shutdown the LocalController we are interacting with.
     */
    public MCRPInteractor shutDown() throws IOException, MCRPException {
         
	     interact(MCRP.SHUT_DOWN.CMD);
	     expect(MCRP.SHUT_DOWN.CODE);
	     return this;
    }

    /**
     * As the LocalController to start a new router.
     */
    public String newRouter(int routerId, int port) throws IOException, MCRPException  {
        return newRouter(routerId,port,port+1);
    }

    /**
     * Ask the LocalController to start a new router.
     */
    public String newRouter(int routerId, int port1, int port2) 
        throws IOException, MCRPException  {
        String toSend = MCRP.NEW_ROUTER.CMD+" "+routerId+ " " + port1 + " " + port2;
        MCRPResponse response = interact(toSend);
        expect(MCRP.NEW_ROUTER.CODE);

        // return the router name
        return response.get(0)[1];
    }

    /** Ask the Local Controller to connect routers */
    public String connectRouters(String host1, int port1, 
      String host2, int port2)throws IOException, MCRPException {
        String toSend = MCRP.CONNECT_ROUTERS.CMD+" "+host1+":"+port1+" "+
          host2+":"+port2;
        MCRPResponse response = interact(toSend);
        expect(MCRP.CONNECT_ROUTERS.CODE);

        // return the connection name
        return response.get(0)[1];
    }

    /** Ask the Local Controller to stop a router */
    public MCRPInteractor endRouter(String host1, int port1) 
        throws IOException, MCRPException
    {
        String toSend= MCRP.ROUTER_SHUT_DOWN.CMD + " "+host1+":"+port1;
        interact(toSend);
        expect(MCRP.ROUTER_SHUT_DOWN.CODE);
        return this;
    }

    /** Set the configuration string for a router */
    public MCRPInteractor setConfigString(String config) throws IOException, 
        MCRPException {
          String toSend = MCRP.ROUTER_CONFIG.CMD + " "+ config;
          interact(toSend);
          expect(MCRP.ROUTER_CONFIG.CODE);
          return this;

    }

  /** Ask the Local Controller to end a link */
    public MCRPInteractor endLink(String host1, int port1, int rId)throws IOException, MCRPException {
        String toSend = MCRP.END_LINK.CMD+" "+host1+":"+port1+" "+rId;
        interact(toSend);
        expect(MCRP.END_LINK.CODE);
        return this;
    }

    /**
     * Ask the LocalController to start a command on a router.
     */
    public String onRouter(int routerId, String className, String[] args) throws IOException, MCRPException  {

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

        MCRPResponse response = interact(toSend);
        expect(MCRP.ON_ROUTER.CODE);

        // return the app name
        return response.get(0)[1];
    }


    /**
     * Check with a local controller.
     */
    public MCRPInteractor checkLocalController(String host, int port) throws IOException, MCRPException {
        String toSend = MCRP.CHECK_LOCAL_CONTROLLER.CMD + 
            " " + host + " " + port;
	      interact(toSend);
	      expect(MCRP.CHECK_LOCAL_CONTROLLER.CODE);
	      return this;
    }
    
    /**
     * Check with a local controller.
     */
    public MCRPInteractor checkLocalController(LocalHostInfo gc) throws IOException, MCRPException {
      String host= gc.getName();
      int port= gc.getPort();
      String toSend = MCRP.CHECK_LOCAL_CONTROLLER.CMD + 
            " " + host + " " + port;
	    interact(toSend);
	  expect(MCRP.CHECK_LOCAL_CONTROLLER.CODE);
	return this;
    }

   /** Send a message to a local controller intended for a router to 
      set its aggregation point */
    public MCRPInteractor setAP(int GID, int APGID) throws IOException, MCRPException {
        String toSend;
        toSend = MCRP.SET_AP.CMD + 
            " " + GID + " " + APGID;
       
	      interact(toSend);
	      expect(MCRP.SET_AP.CODE);
	      return this;
    }
    
    /** Send a message to a local controller informing it about a routers
    status as an aggregation point */
    public MCRPInteractor reportAP(int GID, int AP) throws IOException, MCRPException {
        String toSend = MCRP.REPORT_AP.CMD + 
            " " + GID + " " +AP;
       
	      interact(toSend);
	      expect(MCRP.REPORT_AP.CODE);
	      return this;
    }

    /**
     * Get the stats from a Router.
     */
    public List<String> getRouterStats() throws IOException, MCRPException {
        // 285-1 localnet  InBytes=0 InPackets=0 InErrors=0 InDropped=0 OutBytes=17890 OutPackets=500 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 285-1 /Router-15151-15152/Connection-1  InBytes=66 InPackets=1 InErrors=0 InDropped=0 OutBytes=17956 OutPackets=501 OutErrors=0 OutDropped=0 InQueue=0 OutQueue=0
        // 285 END 2

	MCRPResponse response = interact(MCRP.GET_ROUTER_STATS.CMD);
	expect(MCRP.GET_ROUTER_STATS.CODE);

        // now we convert the replies in the response
	// into a list of connections

        // get no of netifs
	int routerReplies = response.getReplies() - 1;

	// create a list for the names
	List<String> stats = new ArrayList<String>();

	for (int r=0; r < routerReplies; r++) {
	    // pick out the r-th connection
	    stats.add(response.get(r)[1]);
	}

	return stats;
    }



}
