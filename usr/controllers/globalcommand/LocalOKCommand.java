package usr.controllers.globalcommand;

import usr.controllers.*;
import usr.console.MCRP;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

public class LocalOKCommand extends GlobalCommand {
    /**
     * Construct a QuitCommand.
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
