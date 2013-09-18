package usr.globalcontroller.command;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import usr.console.AbstractRestCommand;
import usr.globalcontroller.GlobalController;
import cc.clayman.console.ManagementConsole;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a GlobalController.
 */
public abstract class GlobalCommand extends AbstractRestCommand {
    // The ManagementConsole
    ManagementConsole managementConsole;

    // The RouterController
    GlobalController controller = null;

    /**
     * Construct a Command given a name, a success code, an error code.
     */
    GlobalCommand(String name) {
        super(name);
    }

    /**
     * Construct a Command given a name, a success code, an error code.
     */
    GlobalCommand(String name, int succCode, int errCode) {
        super(name);
    }

    /**
     * Get the ManagementConsole this is a command for.
     */
    @Override
	public ManagementConsole getManagementConsole() {
        return managementConsole;
    }

    /**
     * Set the ManagementConsole this is a command for.
     */
    @Override
	public void setManagementConsole(ManagementConsole mc) {
        managementConsole = mc;
        controller = (GlobalController)managementConsole.getAssociated();
    }

    /**
     * Evaluate the Command.
     * Returns false if there is a problem responding down the channel
     */
    @Override
	public abstract boolean evaluate(Request request, Response response);


}