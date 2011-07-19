package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.applications.ApplicationResponse;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The APP_STOP command stops an application in the same
 * JVM as a Router.
 */
public class AppStopCommand extends RouterCommand {
    /**
     * Construct a AppStopCommand
     */
    public AppStopCommand() {
        super(MCRP.APP_STOP.CMD, MCRP.APP_STOP.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {

        String rest = req.substring(MCRP.APP_STOP.CMD.length()).trim();

        if (rest.equals("")) {
            error("APP_STOP needs application name");
            return false;
        } else {

            ApplicationResponse response = controller.appStop(rest);

            if (response.isSuccess()) {
                success(response.getMessage());
                return true;
            } else {
                error(response.getMessage() + " for " + rest);
                return false;
            }
        }
    }
}
