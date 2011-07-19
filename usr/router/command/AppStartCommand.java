package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.applications.ApplicationResponse;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The APP_START command starts an application in the same
 * JVM as a Router.
 */
public class AppStartCommand extends RouterCommand {
    /**
     * Construct a AppStartCommand
     */
    public AppStartCommand() {
        super(MCRP.APP_START.CMD, MCRP.APP_START.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {

        String rest = req.substring(MCRP.APP_START.CMD.length()).trim();

        if (rest.equals("")) {
            error("APP_START needs application class name");
            return false;
        } else {

            ApplicationResponse response = controller.appStart(rest);

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
