package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.globalcontroller.*;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * A LocalOKCommand.
 */
public class LocalOKCommand extends GlobalCommand {
    /**
     * Construct a LocalOKCommand.
     */
    public LocalOKCommand() {
        super(MCRP.OK_LOCAL_CONTROLLER.CMD, MCRP.OK_LOCAL_CONTROLLER.CODE, 
          MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String []args= req.split(" ");
        if (args.length != 3) {
            error ("Expected three arguments for LocalOKCommand");
            return false;
        }
        String hostName= args[1];
        int port= Integer.parseInt(args[2]);
        LocalHostInfo lh= new LocalHostInfo(hostName, port);
        success("Local OK received from "+args[1]+":"+args[2]);
        controller.aliveMessage(lh);
        return true;
    }

}
