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
 * The SET_AP command selets whether a router is or is not
 * an aggregation point
 */
public class SetAPCommand extends RouterCommand {
    /**
     * Construct a SetAddressCommand.
     */
    public SetAPCommand() {
        super(MCRP.SET_AP.CMD, MCRP.SET_AP.CODE, MCRP.SET_AP.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {

        String[] parts = req.split(" ");

        if (parts.length != 3) {
            error ("SET_AP command requires GID and AP GID");
            return false;
        }
        int GID;
        int AP;
        try {
            GID= Integer.parseInt(parts[1]);
            AP= Integer.parseInt(parts[2]);
        } catch (Exception e) {
            error ("SET_AP command requires GID and AP GID");
            return false;
        }
        success(GID+" has set AP to "+AP);
        controller.setAP(GID, AP);
        return true;
    }

}
