package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
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
public class ReadOptionsStringCommand extends RouterCommand {
    /**
     * Construct a GetAddressCommand.
     */
    public ReadOptionsStringCommand() {
	super(MCRP.READ_OPTIONS_STRING.CMD, MCRP.READ_OPTIONS_STRING.CODE,
	      MCRP.READ_OPTIONS_STRING.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
	boolean result = true;

	String rest = req.substring(MCRP.READ_OPTIONS_STRING.CMD.length()).trim();
	// Logger.getLogger("log").logln(USR.ERROR, "RECEIVED STRING");
	// Logger.getLogger("log").logln(USR.ERROR, rest);

	if (controller.readOptionsString(rest)) {
	    success("Translated Options String");
	    return true;
	}
	error ("Cannot translate XML Options String");
	return false;
    }

}
