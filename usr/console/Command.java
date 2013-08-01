package usr.console;

import usr.router.*;
import usr.logging.*;
import java.nio.channels.SocketChannel;

/**
 * A Command object processes a command handled by the ManagementConsole
 * of a ComponentController.
 */
public interface Command {
    /**
     * Get the name of command as a string.
     */
    public String getName();

    /**
     * Get the ManagementConsole this is a command for.
     */
    public ManagementConsole getManagementConsole();

    /**
     * Set the ManagementConsole this is a command for.
     */
    public void setManagementConsole(ManagementConsole mc);
}