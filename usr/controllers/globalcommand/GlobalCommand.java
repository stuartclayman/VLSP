package usr.controllers.globalcommand;

import usr.controllers.*;
import usr.interactor.*;
import java.nio.channels.SocketChannel;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public interface GlobalCommand extends Command {

    /**
     * Set the ManagementConsole this is a command for.
     */
   public void setManagementConsole(GlobalControllerManagementConsole mc);

}
