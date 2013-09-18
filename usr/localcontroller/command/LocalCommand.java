package usr.localcontroller.command;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import usr.console.AbstractRestCommand;
import usr.localcontroller.LocalController;
import usr.localcontroller.LocalControllerManagementConsole;
import cc.clayman.console.ManagementConsole;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a LocalController
 */
public abstract class LocalCommand extends AbstractRestCommand {
    // The ManagementConsole
    LocalControllerManagementConsole managementConsole;

    // The RouterController
    LocalController controller;


    /**
     * Construct a Command given a name
     */
    LocalCommand(String name) {
        super(name);
    }

    /**
     * Construct a Command given a name, a success code, an error code.
     */
    LocalCommand(String name, int succCode, int errCode) {
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
        managementConsole = (LocalControllerManagementConsole)mc;
        controller = (LocalController)managementConsole.getComponentController();
    }

    /**
     * Evaluate the Command.
     * Returns false if there is a problem responding down the channel
     */
    @Override
	public abstract boolean evaluate(Request request, Response response);


}