package usr.localcontroller.command;

import usr.protocol.MCRP;
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
        int r2;
        r1= new LocalHostInfo(args[1]);
        r2= Integer.parseInt(args[2]);
        if (managementConsole.endLink(r1,r2)) {
            success("LINK ENDED FROM"+r1+" to Id "+r2);
            return true;
        }
        error("CANNOT END LINK");
        return false;
    }

}
