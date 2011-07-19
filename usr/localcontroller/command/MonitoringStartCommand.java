package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.net.InetSocketAddress;

/**
 * The MONITORING_START command starts monitoring on specified address
 * and port, with Measurements every N seconds.
 *
 * MONITORING_START 192.168.7.23:4545 10
 */
public class MonitoringStartCommand extends LocalCommand {
    /**
     * Construct a MonitoringStartCommand
     */
    public MonitoringStartCommand() {
	super(MCRP.MONITORING_START.CMD, MCRP.MONITORING_START.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
	String rest = req.substring(MCRP.MONITORING_START.CMD.length()).trim();

	boolean result;

	String [] parts = rest.split(" ");

	if (parts.length != 2) {
	    result = error("Expected request: MONITORING_START address:port seconds");
	    return result;
	} else {
	    // get address and port
	    // check ip addr spec
	    String[] ipParts = parts[0].split(":");
	    if (ipParts.length != 2) {
		Logger.getLogger("log").logln(USR.ERROR, leadin() + "INVALID MONITORING_START ip address: " + parts[0]);
		result = error("MONITORING_START invalid address: " + parts[0]);
		return result;
	    }

	    // process host and port
	    String host = ipParts[0];

	    Scanner sc = new Scanner(ipParts[1]);
	    int portNumber;

	    try {
		portNumber = sc.nextInt();
	    } catch (Exception e) {
		result = error("MONITORING_START invalid port: " + ipParts[1]);
		return result;
	    }

	    // get timeout for Probe
	    int timeout;

	    // get timeout
	    sc = new Scanner(parts[1]);

	    try {
		timeout = sc.nextInt();
	    }  catch (Exception e) {

		result = error("MONITORING_START invalid timeout: " + parts[1]);
		return result;
	    }


	    // if we get here all the args seem OK
	    InetSocketAddress socketAddress = new InetSocketAddress(host, portNumber);


	    controller.startMonitoring(socketAddress, timeout);

	    result = success("Monitoring Start Accepted");

	    return result;
	}

    }

}
