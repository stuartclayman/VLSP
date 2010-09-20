package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_GLOBAL_ID command.
 */
public class GetGlobalIDCommand extends RouterCommand {
    /**
     * Construct a GetGlobalIDCommand
     */
    public GetGlobalIDCommand() {
        super(MCRP.GET_GLOBAL_ID.CMD, MCRP.GET_GLOBAL_ID.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        int id = controller.getGlobalID();
        
        boolean result = success("" + id);

        if (!result) {
            System.err.println(leadin() + " GET_GLOBAL_ID response failed");
        }

        return result;
    }

}
