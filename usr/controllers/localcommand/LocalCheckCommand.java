package usr.controllers.localcommand;

import usr.interactor.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * The QUIT command.
 */
public class LocalCheckCommand extends AbstractCommand {
    /**
     * Construct a QuitCommand.
     */
    public LocalCheckCommand() {
        super(MCRP.CHECK_LOCAL_CONTROLLER.CMD, MCRP.CHECK_LOCAL_CONTROLLER.CODE, 
          MCRP.CHECK_LOCAL_CONTROLLER.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        
        String []args= req.split(" ");
        if (args.length != 3) {
           error("Local Check Command has wrong arguments");
           return false;
        }
        
        success("Ping from global controller received.");
        
        String hostName= args[1];
        int port= Integer.parseInt(args[2]);
        LocalHostInfo gc= new LocalHostInfo(hostName, port);
        
        managementConsole.contactFromGlobal(gc);

        return true;
    }

}
