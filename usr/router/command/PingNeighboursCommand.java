package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_NAME command.
 */
public class PingNeighboursCommand extends RouterCommand {
    /**
     * Construct a GetNameCommand.
     */
    public PingNeighboursCommand() {
        super(MCRP.PING_NEIGHBOURS.CMD, MCRP.PING_NEIGHBOURS.CODE, 
          MCRP.PING_NEIGHBOURS.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        controller.pingNeighbours();
        success("PINGED NEIGHBOURS");
        return true;
    }

}
