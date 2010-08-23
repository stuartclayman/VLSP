package usr.controllers.globalcommand;

import usr.interactor.*;
import usr.controllers.*;
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
    GlobalControllerManagementConsole managementConsole;

    // The RouterController
    GlobalController controller= null;

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
     * Returns false if there is a problem responding down the channel
     */
    public abstract boolean evaluate(String req);

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
    public void setManagementConsole(GlobalControllerManagementConsole mc) {
        managementConsole = mc;
        controller = managementConsole.getGlobalController();
    }

    /**
     * Respond to the client successfully.
     * Returns false if it cannot send the response.
     */
    boolean success(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSuccessCode());
        sb.append(" ");
        sb.append(s);
        String resp = sb.toString();
        System.err.println("MC: <<< RESPONSE: " + resp);

        return respond(resp);
    }

    /**
     * Respond to the client with an error
     * Returns false if it cannot send the response.
     */
    boolean error(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(getErrorCode());
        sb.append(" ");
        sb.append(s);
        String resp = sb.toString();
        System.err.println("MC: <<< RESPONSE: " + resp);

        return respond(resp);
    }

    /**
     * Send an item of a list response.
     * Returns false if it cannot send the response.
     */
    boolean list(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSuccessCode());
        sb.append("-");
        sb.append(s);
        String resp = sb.toString();
        System.err.println("MC: <<< ITEM: " + resp);

        return respond(resp);
    }


    /**
     * Hash code
     */
    public int hashCode() {
        return name.hashCode();
    }
}
