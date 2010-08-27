package usr.interactor;

import usr.console.MCRP;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import usr.common.LocalHostInfo;
import java.util.List;
import java.util.ArrayList;    /* Calls for ManagementConsole */

public class LocalControllerInteractor extends MCRPInteractor
{
    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public LocalControllerInteractor(String addr, int port) throws UnknownHostException, IOException  {
	initialize(InetAddress.getByName(addr), port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a router.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public LocalControllerInteractor(InetAddress addr, int port) throws UnknownHostException, IOException  {
	initialize(addr, port);
    }

    public LocalControllerInteractor(LocalHostInfo lh) throws UnknownHostException, IOException  {
	initialize(lh.getIp(), lh.getPort());
    }


    public MCRPInteractor respondToGlobalController(LocalHostInfo lc) throws IOException, MCRPException {
      String command= MCRP.OK_LOCAL_CONTROLLER.CMD+" "+lc.getName()+" "+ lc.getPort();
	    interact(command);
	    expect(MCRP.OK_LOCAL_CONTROLLER.CODE);
	    return this;
    }

}
