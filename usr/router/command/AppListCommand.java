package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.Address;
import usr.applications.ApplicationHandle;
import java.util.Collection;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The APP_LIST command.
 */
public class AppListCommand extends RouterCommand {
    /**
     * Construct a AppListCommand
     */
    public AppListCommand() {
        super(MCRP.APP_LIST.CMD, MCRP.APP_LIST.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        Collection<ApplicationHandle> apps = controller.appList();

        int count = 0;
        for (ApplicationHandle appH : apps) {

            list(appH.getName());
            count++;
        }               

        boolean result = success("END " + count);

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "APP_LIST response failed");
        }

        return result;

    }

}
