package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.globalcontroller.*;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * A LocalOKCommand.
 */
public class ShutDownCommand extends GlobalCommand {
    /**
     * Construct a LocalOKCommand.
     */
    public ShutDownCommand() {
        super(MCRP.GC_SHUT_DOWN.CMD, MCRP.GC_SHUT_DOWN.CODE, 
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
