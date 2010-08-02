package usr.router;

import java.nio.channels.SocketChannel;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public interface Command {
    /**
     * Evaluate the Command.
     */
    public void evaluate();

    /**
     * Get the name of command as a string.
     */
    public String getName();

    /**
     * Get the return code on success.
     */
    public int getSuccessCode();

    /**
     * Get the return code on error.
     */
    public int getErrorCode();

    /**
     * Get the SocketChannel this command 
     * is a handler for.
     */
    public SocketChannel getChannel();

    /**
     * Set the ManagementConsole this is a command for.
     */
    public void setManagementConsole(ManagementConsole mc);
}
