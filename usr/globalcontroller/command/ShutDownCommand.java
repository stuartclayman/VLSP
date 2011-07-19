package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.globalcontroller.*;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * A ShutDownCommand
 */
public class ShutDownCommand extends GlobalCommand {
    /**
     * Construct a ShutDownCommand
     */
    public ShutDownCommand() {
	super(MCRP.SHUT_DOWN.CMD, MCRP.SHUT_DOWN.CODE,
	      MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
	controller.shutDownCommand();
	success("Shut Down Sent to Local Controller -- will be processed next in queue");
	return true;
    }

}
