package usr.router.command;

import usr.interactor.*;
import usr.router.*;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public interface RouterCommand extends Command {

    /**
     * Set the ManagementConsole this is a command for.
     */
   public void setManagementConsole(RouterManagementConsole mc);

}
