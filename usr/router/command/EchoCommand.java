package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_NAME command.
 */
public class EchoCommand extends RouterCommand {
    /**
     * Construct a GetNameCommand.
     */
    public EchoCommand() {
        super(MCRP.ECHO.CMD, MCRP.ECHO.CODE, 
          MCRP.ECHO.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        int id;
        try {
            String [] args= req.split(" ");
            if (args.length != 2) {
                error("REQUIRE NUMERIC ID");
                return false;
            }
            id= Integer.parseInt(args[1]);
        } catch (Exception e)
        { 
            error ("CANNOT PARSE INTEGER");
            return false;
        }
        if (controller.echo(id)) {
            success("ECHO SENT TO "+ id);
            return true;
        } 
        error ("No route to router "+id);
        return false;
    }

}
