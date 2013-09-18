package usr.console;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import cc.clayman.console.ManagementConsole;

/**
 * A Command object processes a command handled by the ManagementConsole
 * of a ComponentController.
 */
public abstract class AbstractRestCommand implements RestCommand {
    // The name of the command
    String name;

    /**
     * Construct a RestCommand given a name
     */
    protected AbstractRestCommand(String name) {
        this.name = name;
    }

    /**
     * Evaluate the Command.
     * Returns false if there is a problem responding down the channel
     */
    @Override
	public abstract boolean evaluate(Request request, Response response);

    /**
     * Get the name of command as a string.
     */
    @Override
	public String getName() {
        return name;
    }

    /**
     * Set the name
     */
    protected void setName(String n) {
        name = n;
    }

    /**
     * Get the ManagementConsole this is a command for.
     */
    @Override
	public abstract ManagementConsole getManagementConsole();

    /**
     * Set the ManagementConsole this is a command for.
     */
    @Override
	public abstract void setManagementConsole(ManagementConsole mc);  // to be set in particular subclasses

    /**
     * Hash code
     */
    @Override
	public int hashCode() {
        return name.hashCode();
    }

    /**
     * Create the String to print out before a message
     */
    protected String leadin() {
        final String MC = "MC: ";
        ManagementConsole mc = getManagementConsole();
        ComponentController controller = (ComponentController)mc.getAssociated();

        if (controller == null) {
            return MC;
        } else {
            return controller.getName() + " " + MC;
        }

    }

}