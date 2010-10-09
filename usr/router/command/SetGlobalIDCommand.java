package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

/**
 * The SET_GLOBAL_ID command.
 * SET_GLOBAL_ID id
 * SET_GLOBAL_ID 47
 */
public class SetGlobalIDCommand extends RouterCommand {
    /**
     * Construct a SetGlobalIDCommand
     */
    public SetGlobalIDCommand() {
        super(MCRP.SET_GLOBAL_ID.CMD, MCRP.SET_GLOBAL_ID.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String idStr = req.substring(MCRP.SET_GLOBAL_ID.CMD.length()).trim();

        Scanner scanner = new Scanner(idStr);

        boolean result;

        if (scanner.hasNextInt()) {
            int id = scanner.nextInt();
            boolean idSet = controller.setGlobalID(id);

            if (idSet) {
                result = success("" + id);
            } else {
                result = error("Cannot set Global ID after communication");
            }
        } else {
            result = error("Cannot set Global ID with value " + idStr);
        }

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + getName() + " response failed");
        }

        return result;
    }

}
