package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The RUN command -- allows applications to run on the router
 */
public class RunCommand extends RouterCommand {
    /**
     * Construct a RunCommand.
     */
    public RunCommand() {
        super(MCRP.RUN.CMD, MCRP.RUN.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
    
        String rest = req.substring(MCRP.RUN.CMD.length()).trim();
        if (rest == "") {
            error("Must supply command name");
            return false;
        }
        if (controller.runCommand(rest)) {
            success("Command started");
            return true;
        }
        error("Cannot run command "+rest);
        return false;
    }

}
