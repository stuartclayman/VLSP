package usr.router.command;

import usr.protocol.MCRP;
import usr.applications.ApplicationResponse;
import usr.logging.*;
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
            error("RUN Must supply command name and args");
            return false;
        }

        ApplicationResponse response = controller.runCommand(rest);

        if (response.isSuccess()) {
            success(response.getMessage());
            return true;
        } else {
            error(response.getMessage() + " for " + rest);
            return false;
        }
    }

}
