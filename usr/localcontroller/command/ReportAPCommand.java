package usr.localcontroller.command;

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
 * The REPORT_AP command selets whether a router is or is not
 * an aggregation point
 */
public class ReportAPCommand extends LocalCommand {
    /**
     * Construct a SetAddressCommand.
     */
    public ReportAPCommand() {
        super(MCRP.REPORT_AP.CMD, MCRP.REPORT_AP.CODE, MCRP.REPORT_AP.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
       
        String[] parts = req.split(" ");
                    
        if (parts.length != 3) {
            error ("REPORT_AP command requires GID and AP GID");
            return false;
        }
        int GID;
        int AP;
        try {
            GID= Integer.parseInt(parts[1]);
            AP= Integer.parseInt(parts[2]);
        } catch (Exception e) {
            error ("REPORT_AP command requires GID and AP GID");
            return false; 
        }
        if (controller.reportAP(GID, AP)) {
            success(GID+" reports AP "+AP);
            return true;
        }
        error ("Incorrect GID number "+GID);
        return false;
    }

}
