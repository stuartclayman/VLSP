package usr.router.command;

import usr.console.*;
import usr.logging.*;
import usr.router.*;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public abstract class RouterCommand extends AbstractCommand {
    // The ManagementConsole
    RouterManagementConsole managementConsole;

    // The ComponentController
    RouterController controller;

    /**
     * Construct a Command given a name, a success code, an error code.
     */
    RouterCommand(String name, int succCode, int errCode) {
        super(name, succCode, errCode);
    }

    /**
     * Get the ManagementConsole this is a command for.
     */
    public ManagementConsole getManagementConsole() {
        return managementConsole;
    }

    /**
     * Set the ManagementConsole this is a command for.
     */
    public void setManagementConsole(ManagementConsole mc) {
        managementConsole = (RouterManagementConsole)mc;
        controller = (RouterController)managementConsole.getComponentController();
    }


}
