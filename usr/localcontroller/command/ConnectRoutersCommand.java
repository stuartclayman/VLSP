package usr.localcontroller.command;

import usr.protocol.MCRP;
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
        super(MCRP.CONNECT_ROUTERS.CMD, MCRP.CONNECT_ROUTERS.CODE, MCRP.CONNECT_ROUTERS.CODE);
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
        LocalHostInfo r1,r2;
        r1= new LocalHostInfo(args[1]);
        r2= new LocalHostInfo(args[2]);
        if (managementConsole.connectRouters(r1,r2)) {
            success("ROUTERS CONNECTED "+r1+" "+r2);
            return true;
        }
        error("CANNOT CONNECT ROUTERS");
        return false;
    }

}
