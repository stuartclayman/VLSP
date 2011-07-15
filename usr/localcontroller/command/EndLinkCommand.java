package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * The EndLink command.
 */
public class EndLinkCommand extends LocalCommand {
    /**
     * Construct a EndLinkCommand.
     */
    public EndLinkCommand() {
        super(MCRP.END_LINK.CMD, MCRP.END_LINK.CODE, 
            MCRP.END_LINK.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String []args= req.split(" ");
        if (args.length != 3) {
            error("Expected three arguments for End Link Command");
            return false;
        }
        LocalHostInfo r1;
        String r2Addr;
        try {
            r1= new LocalHostInfo(args[1]);
            r2Addr = args[2];
        } catch (Exception e) {
            error ("CANNOT PARSE HOST INFO FOR END_LINK"+e.getMessage());
            return false;
        }  
        if (controller.endLink(r1,r2Addr)) {
            success("LINK ENDED FROM"+r1+" to Id "+r2Addr);
            return true;
        }
        error("CANNOT END LINK");
        return false;
    }

}
