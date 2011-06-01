package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * The QUIT command.
 */
public class EndRouterCommand extends LocalCommand {
    /**
     * Construct a NewRouterCommand.
     */
    public EndRouterCommand() {
        super(MCRP.ROUTER_SHUT_DOWN.CMD, MCRP.ROUTER_SHUT_DOWN.CODE,     
          MCRP.ROUTER_SHUT_DOWN.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String []args= req.split(" ");
        if (args.length != 2) {
            error("Expected two arguments for End Router Command");
            return false;
        }
        LocalHostInfo lhi= null;
        try {
            lhi = new LocalHostInfo(args[1]);   
            if (lhi == null) {
                error ("LOCAL HUST INFO IN WRONG FORMAT");
                return false;
            } 
        } catch (Exception e) {
            error("Cannot convert "+args[1]+" to host info");
            return false;
        }
        
        if (controller.endRouter(lhi)) {
            success("ROUTER ENDED "+lhi);
            return true;
        }
        error("CANNOT END ROUTER "+lhi);
        return false;
    }

}
