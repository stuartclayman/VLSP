package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.net.AddressFactory;
import usr.net.Address;


/**
 * The ECHO command.
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
        Address addr;
        try {
            String [] args= req.split(" ");
            if (args.length != 2) {
                error("REQUIRE NUMERIC ADDRESS");
                return false;
            }

            addr = AddressFactory.newAddress(args[1].trim());
        } catch (Exception e)
        {
            error ("CANNOT PARSE ADDRESS");
            return false;
        }
        if (controller.echo(addr)) {
            success("ECHO SENT TO "+ addr);
            return true;
        }
        error ("No route to router "+addr);
        return false;
    }

}
