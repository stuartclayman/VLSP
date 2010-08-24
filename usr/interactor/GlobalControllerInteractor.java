package usr.interactor;

import java.net.Socket;
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

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public GlobalControllerInteractor(InetAddress addr, int port) throws UnknownHostException, IOException  {
	initialize(addr, port);
    }

    public MCRPInteractor checkLocalController(String host, int port) throws IOException, MCRPException {
        String toSend = MCRP.CHECK_LOCAL_CONTROLLER.CMD + 
            " " + host + " " + port;
	    interact(toSend);
	  expect(MCRP.CHECK_LOCAL_CONTROLLER.CODE);
	return this;
    }
}
