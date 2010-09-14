package usr.console;

import usr.router.RouterManagementConsole;
import usr.router.RouterController;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * A Command object processes a command handled by the ManagementConsole
 * of a ComponentController.
 */
public abstract class AbstractCommand extends ChannelResponder implements Command {
    // The name of the command
    String name;

    // The success code
    int successCode;

    // The error code
    int errorCode;

    // The ManagementConsole
    // To be set in particular subclasses

    /**
     * Construct a Command given a name, a success code, an error code.
     */
    protected AbstractCommand(String name, int succCode, int errCode) {
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
    public abstract void setManagementConsole(ManagementConsole mc);  // to be set in particular subclasses

    /**
     * Respond to the client successfully.
     * Returns false if it cannot send the response.
     */
    protected boolean success(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSuccessCode());
        sb.append(" ");
        sb.append(s);
        String resp = sb.toString();
        System.out.println(leadin() + "<<< RESPONSE: " + resp);

        return respond(resp);
    }

    /**
     * Respond to the client with an error
     * Returns false if it cannot send the response.
     */
    protected boolean error(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(getErrorCode());
        sb.append(" ");
        sb.append(s);
        String resp = sb.toString();
        System.err.println(leadin() + "<<< RESPONSE: " + resp);

        return respond(resp);
    }

    /**
     * Send an item of a list response.
     * Returns false if it cannot send the response.
     */
    protected boolean list(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSuccessCode());
        sb.append("-");
        sb.append(s);
        String resp = sb.toString();
        System.err.println(leadin() + "<<< ITEM: " + resp);

        return respond(resp);
    }


    /**
     * Hash code
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Create the String to print out before a message
     */
    protected String leadin() {
        final String MC = "MC: ";
        ManagementConsole mc = getManagementConsole();
        ComponentController controller = mc.getComponentController();

        if (controller == null) {
            return MC;
        } else {
            return controller.getName() + " " + MC;
        }

    }


}
