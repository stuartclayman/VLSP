package usr.controllers.localcommand;

import usr.controllers.*;
import usr.interactor.*;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public interface LocalCommand extends Command {

    /**
     * Set the ManagementConsole this is a command for.
     */
   public void setManagementConsole(LocalControllerManagementConsole mc);

}
