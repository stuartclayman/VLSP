package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * The CHECK_LOCAL_CONTROLLER command.
 */
public class LocalCheckCommand extends LocalCommand {
    /**
     * Construct a LocalCheckCommand.
     */
    public LocalCheckCommand() {
        super(MCRP.CHECK_LOCAL_CONTROLLER.CMD, MCRP.CHECK_LOCAL_CONTROLLER.CODE, MCRP.CHECK_LOCAL_CONTROLLER.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {

        String [] args= req.split(" ");
        if (args.length != 3) {
            error("Local Check Command has wrong arguments");
            return false;
        }

        success("Ping from global controller received.");

        String hostName= args[1];
        int port= Integer.parseInt(args[2]);
        LocalHostInfo gc= null;
        try {
            gc = new LocalHostInfo(hostName, port);
        } catch (Exception e) {
            error("Cannot find host info for LOCAL_CHECK_COMMAND "+e.getMessage());
            return false;
        }
        controller.aliveMessage(gc);

        return true;
    }

}
