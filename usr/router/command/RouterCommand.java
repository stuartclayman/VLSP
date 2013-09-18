package usr.router.command;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import usr.console.AbstractRestCommand;
import usr.router.RouterController;
import usr.router.RouterManagementConsole;
import cc.clayman.console.ManagementConsole;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public abstract class RouterCommand extends AbstractRestCommand {
    // The ManagementConsole
    RouterManagementConsole managementConsole;

    // The ComponentController
    RouterController controller;

    /**
     * Construct a Command given a name
     */
    RouterCommand(String name) {
        super(name);
    }

    /**
     * Construct a Command given a name, a success code, an error code.
     */
    RouterCommand(String name, int succCode, int errCode) {
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
        managementConsole = (RouterManagementConsole)mc;
        controller = (RouterController)managementConsole.getComponentController();
    }

    /**
     * Evaluate the Command.
     * Returns false if there is a problem responding down the channel
     */
    @Override
	public abstract boolean evaluate(Request request, Response response);



}