package usr.router.command;

import usr.protocol.MCRP;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.UnknownHostException;

/**
 * The READ_OPTIONS_FILE command
 */
public class ReadOptionsFileCommand extends RouterCommand {
    /**
     * Construct a GetAddressCommand.
     */
    public ReadOptionsFileCommand() {
        super(MCRP.READ_OPTIONS_FILE.CMD, MCRP.READ_OPTIONS_FILE.CODE,
           MCRP.READ_OPTIONS_FILE.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        boolean result = true;

        String[] args= req.split(" ");
                    
        if (args.length != 2) {
            error("READ_OPTIONS_FILE requires two arguments");
            return false;
        }
        if(controller.readOptionsFile(args[1].trim())) {
            success("Read Options File");
            return true;
        }
        error ("Cannot read options file");
        return true;
    }

}
