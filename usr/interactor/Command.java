package usr.interactor;

import usr.controllers.*;
import usr.router.*;
import java.nio.channels.SocketChannel;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public interface Command {
    /**
     * Evaluate the Command.
     * Returns false if there is a problem responding down the channel
     */
    public boolean evaluate(String request);

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
     * Set the SocketChannel this command 
     */
    public void setChannel(SocketChannel ch);

    /**
     * Set the ManagementConsole this is a command for.
     */
    public abstract void setManagementConsole(RouterManagementConsole mc);
    public abstract void setManagementConsole(LocalControllerManagementConsole mc);
    public abstract void setManagementConsole(GlobalControllerManagementConsole mc);
}
