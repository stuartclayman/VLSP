package usr.interactor;

import usr.protocol.MCRP;
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
    public MCRPInteractor newRouter(int routerId) throws IOException, MCRPException  {
        String toSend = MCRP.NEW_ROUTER.CMD+" "+routerId;
        interact(toSend);
        expect(MCRP.NEW_ROUTER.CODE);
        return this;
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

}
