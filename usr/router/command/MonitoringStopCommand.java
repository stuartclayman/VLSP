package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.net.InetSocketAddress;

/**
 * The MONITORING_STOP command stops monitoring
 * MONITORING_STOP
 */
public class MonitoringStopCommand extends RouterCommand {
    /**
     * Construct a MonitoringStopCommand
     */
    public MonitoringStopCommand() {
        super(MCRP.MONITORING_STOP.CMD, MCRP.MONITORING_STOP.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        controller.stopMonitoring();

        boolean result = success("Monitoring Stopped");

        return result;
    }
}
