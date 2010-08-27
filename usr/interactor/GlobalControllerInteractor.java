package usr.interactor;

import usr.console.MCRP;
import java.net.Socket;
import usr.common.LocalHostInfo;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;    /* Calls for ManagementConsole */

public class GlobalControllerInteractor extends MCRPInteractor
{
    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public GlobalControllerInteractor(String addr, int port) throws UnknownHostException, IOException  {
	initialize(InetAddress.getByName(addr), port);
    }

    public GlobalControllerInteractor(InetAddress addr, int port) throws UnknownHostException, IOException  {
	initialize(addr, port);
    }
    
    public GlobalControllerInteractor(LocalHostInfo lh) throws UnknownHostException, IOException  {
	initialize(lh.getIp(), lh.getPort());
    }
    
    public MCRPInteractor shutDown() throws IOException, MCRPException {
         
	    interact(MCRP.SHUT_DOWN.CMD);
	     expect(MCRP.SHUT_DOWN.CODE);
	     return this;
    }

    public MCRPInteractor newRouter(int routerId) throws IOException, MCRPException  {
        String toSend = MCRP.NEW_ROUTER.CMD+" "+routerId;
        interact(toSend);
        expect(MCRP.NEW_ROUTER.CODE);
        return this;
    }

    public MCRPInteractor checkLocalController(String host, int port) throws IOException, MCRPException {
        String toSend = MCRP.CHECK_LOCAL_CONTROLLER.CMD + 
            " " + host + " " + port;
	    interact(toSend);
	  expect(MCRP.CHECK_LOCAL_CONTROLLER.CODE);
	return this;
    }
    
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
