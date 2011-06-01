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
        String []args= req.split(" ");
        if (args.length != 3) {
            error("Expected three arguments for Connect Routers Command");
            return false;
        }
        LocalHostInfo r1= null,r2= null;
        try {
            r1= new LocalHostInfo(args[1]);
            r2= new LocalHostInfo(args[2]);

        } catch (Exception e) {
            error ("CANNOT DECODE HOST INFO FOR CONNECT ROUTER COMMAND"+e.getMessage());
            return false;
        }
        String connectionName = controller.connectRouters(r1,r2);
        if (connectionName != null) {
            success(connectionName); // WAS success("ROUTERS CONNECTED "+r1+" "+r2);
            return true;
        } else {
            error("CANNOT CONNECT ROUTERS");
            return false;
        }
    }

}
