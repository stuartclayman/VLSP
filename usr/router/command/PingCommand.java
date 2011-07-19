package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.net.AddressFactory;
import usr.net.Address;


/**
 * The PING command.
 */
public class PingCommand extends RouterCommand {
    /**
     * Construct a GetNameCommand.
     */
    public PingCommand() {
        super(MCRP.PING.CMD, MCRP.PING.CODE, MCRP.PING.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        Address addr;
        try {
            String [] args= req.split(" ");
            if (args.length != 2) {
                error("REQUIRE ADDRESS");
                return false;
            }

            addr = AddressFactory.newAddress(args[1].trim());
        } catch (Exception e) {
            error ("CANNOT PARSE ADDRESS");
            return false;
        }

        if (controller.ping(addr)) {
            success("PING SENT TO "+ addr);
            return true;
        }
        error ("No route to router "+addr);
        return false;
    }

}
