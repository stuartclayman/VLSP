package usr.router.command;

import usr.router.Command;
import usr.router.ManagementConsole;
import usr.router.RouterController;
import usr.router.ChannelResponder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * A Command processes a command handled by the ManagementConsole
 * of a Router.
 */
public abstract class AbstractCommand extends ChannelResponder implements Command {
    // The name of the command
    String name;

    // The success code
    int successCode;

    // The error code
    int errorCode;

    // The ManagementConsole
    ManagementConsole managementConsole;

    // The RouterController
    RouterController controller;

    /**
     * Construct a Command given a success code, an error code
     * and the SocketChannel.
     */
    AbstractCommand(String name, int succCode, int errCode) {
        successCode = succCode;
        errorCode = errCode;
        this.name = name;
    }

    /**
     * Evaluate the Command.
     */
    public abstract void evaluate(String req);

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
     * Set the ManagementConsole this is a command for.
     */
    public void setManagementConsole(ManagementConsole mc) {
        managementConsole = mc;
        controller = managementConsole.getRouterController();
    }

    /**
     * Respond to the client successfully
     */
    void success(String s) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(getSuccessCode());
        sb.append(" ");
        sb.append(s);
        sb.append("\n");
        String resp = sb.toString();
        System.err.print("MC: <<< RESPONSE: " + resp);

        respond(resp);
    }

    /**
     * Respond to the client with an error
     */
    void error(String s) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(getErrorCode());
        sb.append(" ");
        sb.append(s);
        sb.append("\n");
        String resp = sb.toString();
        System.err.print("MC: <<< RESPONSE: " + resp);


        respond(resp);
    }

    /**
     * Hash code
     */
    public int hashCode() {
        return name.hashCode();
    }
}
