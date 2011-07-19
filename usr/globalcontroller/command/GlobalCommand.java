package usr.globalcontroller.command;

import usr.globalcontroller.*;
import usr.logging.*;
import usr.console.*;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a GlobalController.
 */
public abstract class GlobalCommand extends AbstractCommand {
    // The ManagementConsole
    GlobalControllerManagementConsole managementConsole;

    // The RouterController
    GlobalController controller= null;

    /**
     * Construct a Command given a name, a success code, an error code.
     */
    GlobalCommand(String name, int succCode, int errCode) {
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
        managementConsole = (GlobalControllerManagementConsole)mc;
        controller = (GlobalController)managementConsole.getComponentController();
    }

}
