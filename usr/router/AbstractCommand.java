package usr.router;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public abstract class AbstractCommand implements Command {
    // The name of the command
    String name;

    // The success code
    int successCode;

    // The error code
    int errorCode;

    // The SocketChannel
    SocketChannel channel;

    // The ManagementConsole
    ManagementConsole managementConsole;

    // The RouterController
    RouterController controller;

    /**
     * Construct a Command given a success code, an error code
     * and the SocketChannel.
     */
    AbstractCommand(String name, int succCode, int errCode, SocketChannel sc) {
        successCode = succCode;
        errorCode = errCode;
        channel = sc;
        this.name = name;
    }

    /**
     * Evaluate the Command.
     */
    public abstract void evaluate();

    /**
     * Get the name of command as a string.
     */
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
     * Get the return code on success.
     */
    public int getSuccessCode() {
        return successCode;
    }

    /**
     * Get the return code on error.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Get the SocketChannel this command 
     * is a handler for.
     */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
     * Set the ManagementConsole this is a command for.
     */
    public void setManagementConsole(ManagementConsole mc) {
        managementConsole = mc;
        controller = managementConsole.getRouterController();
    }

    /**
     * Respond to the client
     */
    void respond(String s) throws IOException {
        s = s.concat("\n");
        System.err.print("MC: <<< RESPONSE: " + s);
        channel.write(ByteBuffer.wrap(s.getBytes()));
    }

}
