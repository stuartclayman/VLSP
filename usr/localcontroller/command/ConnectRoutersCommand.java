package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * The ConnectRouters command.
 */
public class ConnectRoutersCommand extends LocalCommand {
    /**
     * Construct a ConnectRoutersCommand.
     */
    public ConnectRoutersCommand() {
	super(MCRP.CONNECT_ROUTERS.CMD, MCRP.CONNECT_ROUTERS.CODE, MCRP.CONNECT_ROUTERS.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
	String [] args= req.split(" ");
	if (args.length != 4 && args.length !=5) {
	    error("Expected four or five arguments for Connect Routers Command");
	    return false;
	}

	LocalHostInfo r1= null,r2= null;
	int weight;

	try {
	    r1= new LocalHostInfo(args[1]);
	    r2= new LocalHostInfo(args[2]);
	    weight= Integer.parseInt(args[3]);

	} catch (NumberFormatException nfe) {
	    error ("BAD weight for link: "+nfe.getMessage());
	    return false;

	} catch (Exception e) {
	    error ("CANNOT DECODE HOST INFO FOR CONNECT ROUTER COMMAND"+e.getMessage());
	    return false;
	}

	String name = null;
	if (args.length == 5) {
	    // there is a name too
	    name = args[4];
	}


	String connectionName = controller.connectRouters(r1,r2, weight, name);

	if (connectionName != null) {
	    success(connectionName); // WAS success("ROUTERS CONNECTED "+r1+" "+r2);
	    return true;
	} else {
	    error("CANNOT CONNECT ROUTERS");
	    return false;
	}
    }

}
